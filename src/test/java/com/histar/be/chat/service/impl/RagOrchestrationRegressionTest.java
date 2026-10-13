package com.histar.be.chat.service.impl;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.auth.service.EmailVerifiedGuard;
import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.service.CharacterService;
import com.histar.be.chat.dto.*;
import com.histar.be.chat.service.*;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.conversation.entity.Conversation;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.observability.ProductionMetrics;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.narrative.NarrativeInvestigationService;
import com.histar.be.rag.service.RagChatService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagOrchestrationRegressionTest {
    @Mock CharacterService characters;
    @Mock LocationService locations;
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock ChatLlmClient directLlm;
    @Mock RagAiChatClient ai;
    @Mock ChatRateLimiter limiter;
    @Mock UsageQuotaService quota;
    @Mock ProfileRepository profiles;
    @Mock UserArtifactRepository artifacts;
    @Mock UserDiscoveryRepository discoveries;
    @Mock PlayerStoryContextService context;
    @Mock EmailVerifiedGuard email;
    @Mock RagChatService semantic;
    @Mock ProductionMetrics metrics;
    @Mock NarrativeInvestigationService quests;
    @InjectMocks ChatServiceImpl service;
    final UUID user = UUID.randomUUID(), character = UUID.randomUUID(), location = UUID.randomUUID(), conversation = UUID.randomUUID();

    @BeforeEach void setup() {
        when(characters.findEnabledChatAgentById(character)).thenReturn(CharacterEntity.builder()
                .id(character).locationId(location).name("Chrono").personaPrompt("persona").build());
        when(locations.findById(location)).thenReturn(Location.builder().id(location).name("Cu Chi")
                .siteCode("cu-chi").sources("Generic location source").build());
        when(conversations.findById(conversation)).thenReturn(Optional.of(Conversation.builder()
                .id(conversation).userId(user).characterId(character).build()));
    }

    private void answer(RagChatResponse response) {
        when(ai.generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(), any()))
                .thenReturn(response);
    }

    @Test void legacyUsesRagAndSendsCurrentQuestionExactlyOnceBeforePersistence() {
        when(messages.findByConversationIdOrderByCreatedAt(conversation)).thenReturn(List.of(
                Message.builder().role("user").content("previous question").build()));
        when(ai.generateWithSources(eq("current question"), any(), any(), any(), any(), any(),
                eq(List.of(Map.of("role", "user", "content", "previous question"))), anyInt(), anyLong(), anyLong(), any()))
                .thenAnswer(invocation -> {
                    verify(messages, never()).save(any());
                    return new RagChatResponse("grounded answer", List.of());
                });
        var result = service.chat(user, new ChatRequest(character, "current question", conversation));
        assertThat(result.reply()).isEqualTo("grounded answer");
        verifyNoInteractions(directLlm);
        verify(email).assertEmailVerified(user);
        verify(limiter).checkAndIncrement(user);
        verify(limiter).recordSuccess(user);
        verify(messages, times(2)).save(any());
    }

    @Test void ragFailureNeverFallsBackPersistsOrAwardsQuestSuccess() {
        when(ai.generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(), any()))
                .thenThrow(new BusinessRuleException("rag unavailable"));
        assertThatThrownBy(() -> service.sendOrchestrated(user, new ChatMessageRequest(character, "question", conversation,
                null, "cu-chi", UUID.randomUUID(), "interview", 1))).hasMessage("rag unavailable");
        verifyNoInteractions(directLlm, quests);
        verify(messages, never()).save(any());
        verify(limiter, never()).recordSuccess(any());
        verify(metrics, never()).recordChat(any(), any(), eq(true));
    }

    @Test void emptyAiSourcesStayEmptyForPremiumUser() {
        answer(new RagChatResponse("insufficient evidence", List.of()));
        lenient().when(quota.shouldIncludeChatSources(user, "cu-chi")).thenReturn(true);
        assertThat(service.sendOrchestrated(user, new ChatMessageRequest(character, "question", conversation)).sources()).isEmpty();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"no_source", "guard"})
    void preservesAiOutcomeMetricLabels(String outcome) {
        answer(new RagChatResponse("answer", List.of(), outcome));
        service.sendOrchestrated(user, new ChatMessageRequest(character, "question", conversation));
        verify(metrics).recordChat(any(), eq(outcome), eq(true));
    }
}
