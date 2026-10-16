package com.histar.be.discovery.service;

import com.histar.be.character.repository.CharacterRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.conversation.repository.ConversationRepository;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.message.repository.MessageRepository;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.repository.QuestStepRepository;
import com.histar.be.quest.service.QuestUnlockGate;
import com.histar.be.quest.service.QuestVisibility;
import jakarta.validation.constraints.*;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A browser key alone cannot attest to a quest dialogue. Require its persisted exchange. */
@Service @RequiredArgsConstructor
public class VerifiedDialogueService {
    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final CharacterRepository characters;
    private final QuestStepRepository steps;
    private final QuestVisibility visibility;
    private final QuestUnlockGate gate;
    private final ProfilePointsService points;
    private final DiscoveryRewardService rewards;
    private final TrialEntitlementGuard trial;

    public record Request(@NotNull UUID locationId, @NotNull UUID conversationId,
            @NotBlank @Size(max = 128) String key) {}

    @Transactional
    public RecordDiscoveryResponse complete(UUID user, Request request) {
        trial.assertStudentCanWrite(user);
        points.lockAccount(user);
        if (!request.key().startsWith("dialogue:")) throw invalid();
        var conversation = conversations.findById(request.conversationId()).orElseThrow(this::invalid);
        if (!user.equals(conversation.getUserId())) throw invalid();
        var character = characters.findById(conversation.getCharacterId()).orElseThrow(this::invalid);
        if (!request.locationId().equals(character.getLocationId())) throw invalid();
        var history = messages.findByConversationIdOrderByCreatedAt(conversation.getId());
        var matching = steps.findByUnlockKey(request.key()).stream()
                .filter(s -> s.getQuest() != null && s.getQuest().getDeletedAt() == null
                        && request.locationId().equals(s.getQuest().getLocationId()))
                .filter(s -> visibility.visible(s.getQuest()))
                .filter(s -> s.getChatPrompt() != null && !s.getChatPrompt().isBlank())
                .filter(s -> {
                    // Match an actual user prompt followed by an assistant reply, not a local flag.
                    for (int i = 0; i < history.size(); i++) {
                        var message = history.get(i);
                        if (!"user".equals(message.getRole()) || message.getContent() == null
                                || !normalize(message.getContent()).contains(normalize(s.getChatPrompt()))) continue;
                        for (int j = i + 1; j < history.size(); j++) {
                            if ("user".equals(history.get(j).getRole())) break;
                            if ("assistant".equals(history.get(j).getRole()) && history.get(j).isServerGenerated()) return true;
                        }
                    }
                    return false;
                }).findFirst().orElseThrow(this::invalid);
        gate.assertUnlocked(user, matching.getQuest());
        return rewards.record(user, request.locationId(), request.key(), "verified_dialogue");
    }

    private String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim(); }
    private BusinessRuleException invalid() { return new BusinessRuleException("Chưa có hội thoại đã lưu phù hợp với nhiệm vụ này."); }
}
