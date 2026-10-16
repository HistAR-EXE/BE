package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.histar.be.character.repository.CharacterRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.conversation.entity.Conversation;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.discovery.service.DiscoveryRewardService;
import com.histar.be.discovery.service.VerifiedDialogueService;
import com.histar.be.message.entity.Message;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.service.QuestUnlockGate;
import com.histar.be.quest.service.QuestVisibility;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerifiedDialogueServiceTest {
    @Mock ConversationRepository conversations;
    @Mock MessageRepository messages;
    @Mock CharacterRepository characters;
    @Mock QuestStepRepository steps;
    @Mock QuestVisibility visibility;
    @Mock QuestUnlockGate gate;
    @Mock ProfilePointsService points;
    @Mock DiscoveryRewardService rewards;
    @Mock TrialEntitlementGuard trial;
    @InjectMocks VerifiedDialogueService service;

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void onlyActiveQuestCanAuthorizeDialogueReward(boolean deleted) {
        UUID user = UUID.randomUUID(), location = UUID.randomUUID(), conversationId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        var quest = Quest.builder().id(UUID.randomUUID()).locationId(location).deletedAt(deleted ? Instant.now() : null).build();
        var step = QuestStep.builder().quest(quest).unlockKey("dialogue:test").chatPrompt("Canonical prompt").build();
        when(conversations.findById(conversationId)).thenReturn(Optional.of(Conversation.builder()
                .id(conversationId).userId(user).characterId(characterId).build()));
        var character = mock(com.histar.be.character.entity.CharacterEntity.class);
        when(character.getLocationId()).thenReturn(location);
        when(characters.findById(characterId)).thenReturn(Optional.of(character));
        when(steps.findByUnlockKey("dialogue:test")).thenReturn(List.of(step));
        when(messages.findByConversationIdOrderByCreatedAt(conversationId)).thenReturn(List.of(
                Message.builder().role("user").content("Canonical prompt").build(),
                Message.builder().role("assistant").content("Answer").serverGenerated(true).build()));
        lenient().when(visibility.visible(quest)).thenReturn(true);
        lenient().when(rewards.record(user, location, "dialogue:test", "verified_dialogue"))
                .thenReturn(new RecordDiscoveryResponse(true, 10, List.of(), null, List.of()));

        var request = new VerifiedDialogueService.Request(location, conversationId, "dialogue:test");
        if (deleted) {
            assertThrows(BusinessRuleException.class, () -> service.complete(user, request));
            verifyNoInteractions(rewards);
        } else {
            assertEquals(10, service.complete(user, request).xpEarned());
            verify(rewards).record(user, location, "dialogue:test", "verified_dialogue");
        }
    }
}
