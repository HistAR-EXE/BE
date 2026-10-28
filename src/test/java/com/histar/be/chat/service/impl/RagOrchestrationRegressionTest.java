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
import com.histar.be.chat.knowledge.ChatKnowledgeIntent;
import com.histar.be.chat.knowledge.ChatKnowledgeRoute;
import com.histar.be.chat.knowledge.ChatPageContext;
import com.histar.be.chat.service.*;
import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
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
import com.histar.be.rag.dto.RagChatAnswer;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RagOrchestrationRegressionTest {
    @Mock org.springframework.transaction.PlatformTransactionManager transactionManager;
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
    @Mock StationChatContextResolver stationContexts;
    @InjectMocks ChatServiceImpl service;
    final UUID user = UUID.randomUUID(), character = UUID.randomUUID(), location = UUID.randomUUID(), conversation = UUID.randomUUID();

    @BeforeEach void setup() {
        when(characters.findEnabledChatAgentById(character)).thenReturn(CharacterEntity.builder()
                .id(character).locationId(location).name("Chrono").personaPrompt("persona").build());
        org.mockito.Mockito.lenient().when(locations.findById(location)).thenReturn(Location.builder().id(location).name("Cu Chi")
                .siteCode("cu-chi").sources("Generic location source").build());
        when(conversations.findById(conversation)).thenReturn(Optional.of(Conversation.builder()
                .id(conversation).userId(user).characterId(character).build()));
    }

    private void answer(RagChatResponse response) {
        when(ai.generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(),
                any(), any(), any()))
                .thenReturn(response);
    }

    @Test void legacyUsesRagAndSendsCurrentQuestionExactlyOnceBeforePersistence() {
        when(messages.findByConversationIdOrderByCreatedAt(conversation)).thenReturn(List.of(
                Message.builder().role("user").content("previous question").build()));
        when(ai.generateWithSources(eq("current question"), any(), any(), any(), any(), any(),
                eq(List.of(Map.of("role", "user", "content", "previous question"))), anyInt(), anyLong(), anyLong(),
                any(), any(), any()))
                .thenAnswer(invocation -> {
                    verify(messages, never()).save(any());
                    return new RagChatResponse("grounded answer", List.of());
                });
        var result = service.chat(user, new ChatRequest(character, "current question", conversation));
        assertThat(result.reply()).isEqualTo("grounded answer");
        verifyNoInteractions(directLlm);
        verify(email).assertEmailVerified(user);
        verify(limiter).reserve(user);
        verify(limiter).settle(user, null);
        verify(messages, times(2)).save(any());
    }

    @Test void ragFailureNeverFallsBackPersistsOrAwardsQuestSuccess() {
        when(ai.generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(),
                any(), any(), any()))
                .thenThrow(new BusinessRuleException("rag unavailable"));
        assertThatThrownBy(() -> service.sendOrchestrated(user, new ChatMessageRequest(character, "question", conversation,
                null, "cu-chi", UUID.randomUUID(), "interview", 1))).hasMessage("rag unavailable");
        verifyNoInteractions(directLlm, quests);
        verify(messages, never()).save(any());
        verify(limiter, never()).settle(any(), any());
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

    @Test void productHelpBypassesHeritagePgvectorAndKeepsPlatformSourcesVisibleWithoutSubscription() {
        answer(new RagChatResponse("Mở bộ sưu tập để xem cổ vật.", List.of(
                new ChatSource("Hướng dẫn sử dụng TimeLens · Cổ vật", "Cách dùng", null, "PLATFORM"))));
        var result = service.sendOrchestrated(
                user, new ChatMessageRequest(character, "Cổ vật dùng để làm gì?", conversation));

        assertThat(result.sources()).singleElement().extracting(ChatSource::scope).isEqualTo("PLATFORM");
        verifyNoInteractions(semantic);
        var route = ArgumentCaptor.forClass(ChatKnowledgeRoute.class);
        verify(ai).generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(),
                any(), any(), route.capture());
        assertThat(route.getValue().intent()).isEqualTo(ChatKnowledgeIntent.PRODUCT_HELP);
        assertThat(route.getValue().scopes()).containsExactly(ChatKnowledgeRoute.KnowledgeScope.PLATFORM);
    }

    @Test void heritageFactKeepsPgvectorFirstAndDoesNotCallAiWhenGrounded() {
        when(semantic.tryAnswer(any())).thenReturn(Optional.of(new RagChatAnswer(
                "Địa đạo được xây dựng qua nhiều giai đoạn [1].",
                List.of(new ChatSource("Tư liệu lịch sử", "Trích đoạn", null)), false, false)));
        when(quota.shouldIncludeChatSources(user, "cu-chi")).thenReturn(true);

        var result = service.sendOrchestrated(
                user, new ChatMessageRequest(character, "Địa đạo được xây dựng năm nào?", conversation));

        assertThat(result.reply()).contains("[1]");
        verify(semantic).tryAnswer(any());
        verifyNoInteractions(ai);
    }

    @Test void mixedQuestionBypassesPgvectorAndSendsBothKnowledgeScopes() {
        answer(new RagChatResponse("answer", List.of()));
        var pageContext = new ChatPageContext(
                "HISTORICAL_REENACTMENT", "/sites/cu-chi/stations/ST01/media", "cu-chi", "ST01");
        var verifiedStation = new StationChatContext("cu-chi", "ST01", "Phòng họp", "verified");
        when(stationContexts.resolve("cu-chi", "ST01")).thenReturn(Optional.of(verifiedStation));

        service.sendOrchestrated(user, new ChatMessageRequest(
                character,
                "Cách xem phim 3D về trận Cedar Falls diễn ra thế nào?",
                conversation,
                "ST01",
                "cu-chi",
                null,
                null,
                null,
                pageContext));

        verifyNoInteractions(semantic);
        var station = ArgumentCaptor.forClass(StationChatContext.class);
        var route = ArgumentCaptor.forClass(ChatKnowledgeRoute.class);
        verify(ai).generateWithSources(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyLong(), anyLong(),
                any(), station.capture(), route.capture());
        assertThat(station.getValue()).isEqualTo(verifiedStation);
        assertThat(route.getValue().intent()).isEqualTo(ChatKnowledgeIntent.MIXED);
        assertThat(route.getValue().scopes()).containsExactlyInAnyOrder(
                ChatKnowledgeRoute.KnowledgeScope.PLATFORM,
                ChatKnowledgeRoute.KnowledgeScope.HERITAGE);
        assertThat(route.getValue().pageContext()).isEqualTo(pageContext.normalized());
    }

    @Test void userProgressBypassesPgvectorAndForwardsLivePlayerContext() {
        var liveState = Map.<String, Object>of(
                "remainingQuestNames", List.of("Mật thư", "Dẫn đường"),
                "checkedInStations", List.of("ST01"));
        when(context.build(user, location)).thenReturn(liveState);

        var response = service.sendOrchestrated(
                user, new ChatMessageRequest(character, "Tôi còn nhiệm vụ nào?", conversation));

        assertThat(response.reply()).isEqualTo("Bạn còn 2 nhiệm vụ chưa hoàn thành: Mật thư, Dẫn đường.");
        assertThat(response.userStateGrounded()).isTrue();
        verifyNoInteractions(ai, semantic);
    }

    @Test void invalidStationFailsBeforeEitherRetrievalPath() {
        when(stationContexts.resolve("cu-chi", "ST99")).thenThrow(new BaseException(
                ErrorCode.INVALID_STATION_CONTEXT, "invalid station"));

        assertThatThrownBy(() -> service.sendOrchestrated(
                user, new ChatMessageRequest(character, "Cổ vật dùng để làm gì?", conversation, "ST99", "cu-chi")))
                .isInstanceOfSatisfying(BaseException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATION_CONTEXT));

        verifyNoInteractions(semantic, ai);
    }

    @Test void mixedSourcesExposePlatformButKeepHistoricalSourcesBehindSubscription() {
        answer(new RagChatResponse("answer", List.of(
                new ChatSource("Hướng dẫn sử dụng TimeLens", "product", null, "PLATFORM"),
                new ChatSource("Tư liệu lịch sử", "history", null, "HERITAGE"))));
        when(quota.shouldIncludeChatSources(user, "cu-chi")).thenReturn(false);

        var result = service.sendOrchestrated(user, new ChatMessageRequest(
                character, "Cách xem phim 3D về trận Cedar Falls diễn ra thế nào?", conversation));

        assertThat(result.sources()).extracting(ChatSource::scope).containsExactly("PLATFORM");
    }

    @Test void streamingProductHelpAlsoBypassesHeritagePgvector() {
        answer(new RagChatResponse("answer", List.of()));
        List<ChatStreamChunk> streamed = new ArrayList<>();

        var prepared = service.prepareOrchestratedStreaming(
                user, new ChatMessageRequest(character, "Tour 360 dùng để làm gì?", conversation), streamed::add);

        assertThat(prepared.reply()).isEqualTo("answer");
        assertThat(streamed).isEmpty();
        verifyNoInteractions(semantic);
    }
    @Test void providerFailureReleasesReservedCapacityWithoutCharge() {
        UUID token=UUID.randomUUID();when(limiter.reserve(user)).thenReturn(token);
        when(ai.generateWithSources(any(),any(),any(),any(),any(),any(),any(),anyInt(),anyLong(),anyLong(),any(),any(),any()))
                .thenThrow(new BusinessRuleException("provider failed"));
        assertThatThrownBy(()->service.sendOrchestrated(user,new ChatMessageRequest(character,"question",conversation))).hasMessage("provider failed");
        verify(limiter).release(user,token);verify(limiter,never()).settle(any(),any());verify(messages,never()).save(any());
    }
    @Test void legacySyncSettlesReservationAndAlwaysReleasesTheLease() {
        UUID token=UUID.randomUUID();when(limiter.reserve(user)).thenReturn(token);
        service.sync(user,new ChatSyncRequest(character,conversation,"question","server response"));
        verify(limiter).settle(user,token);verify(limiter).release(user,token);verify(messages,times(2)).save(any());
    }
    @Test void exemptQuestHintsNeverReserveOrChargeChatCapacity() {
        answer(new RagChatResponse("hint",List.of()));
        service.sendOrchestrated(user,new ChatMessageRequest(character,"Nắp hầm Củ Chi được ngụy trang thế nào?",conversation));
        verify(limiter,never()).reserve(any());verify(limiter,never()).settle(any(),any());
    }
    @Test void streamingPublisherCancellationReleasesReservationBeforePersistence() {
        UUID token=UUID.randomUUID();when(limiter.reserve(user)).thenReturn(token);
        when(semantic.tryAnswerStreaming(any(),any())).thenAnswer(invocation->{
            java.util.function.Consumer<com.histar.be.rag.dto.RagStreamChunk> publisher=invocation.getArgument(1);
            publisher.accept(new com.histar.be.rag.dto.RagStreamChunk("chunk",List.of(new ChatSource("source","evidence",null,"PLATFORM"))));
            return Optional.empty();
        });
        assertThatThrownBy(()->service.prepareOrchestratedStreaming(user,new ChatMessageRequest(character,"Địa đạo được xây dựng năm nào?",conversation),chunk->{throw new IllegalStateException("disconnected");})).hasMessage("disconnected");
        verify(limiter).release(user,token);verify(limiter,never()).settle(any(),any());verify(messages,never()).save(any());
    }

}
