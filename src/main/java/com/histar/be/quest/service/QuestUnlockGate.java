package com.histar.be.quest.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.QuestStatus;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.rules.QuestRules;
import com.histar.be.profile.service.PremiumContentPolicy;
import com.histar.be.profile.service.TierAccessService;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Điều kiện mở nhiệm vụ — BE là nơi duy nhất kiểm tra (FE chỉ hiển thị lý do). */
@Component
@RequiredArgsConstructor
public class QuestUnlockGate {

    private final UserDiscoveryRepository userDiscoveryRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final QuestRepository questRepository;
    private final TierAccessService tierAccess;

    public void assertUnlocked(UUID userId, Quest quest) {
        if (PremiumContentPolicy.premiumQuest(quest) && !tierAccess.hasPremiumAccess(userId)) {
            throw new BusinessRuleException("Nhiệm vụ Online này cần Premium.");
        }
        UUID afterQuestId = quest.getUnlockAfterQuestId();
        if (afterQuestId != null && !isCompleted(userId, afterQuestId)) {
            String title = questRepository.findById(afterQuestId).map(Quest::getTitle).orElse("nhiệm vụ trước");
            throw new BusinessRuleException("Hoàn thành “" + title + "” để mở nhiệm vụ này.");
        }
        var keys = QuestRules.parseKeys(quest.getUnlockDiscoveryKeys());
        if (keys.isEmpty()) {
            return;
        }
        Set<String> have = userDiscoveryRepository
                .findByUserIdAndLocationId(userId, quest.getLocationId())
                .stream()
                .map(UserDiscovery::getDiscoveryKey)
                .collect(Collectors.toSet());
        if (!have.containsAll(keys)) {
            throw new BusinessRuleException("Nhiệm vụ này mở sau khi bạn khám phá đủ các điểm yêu cầu.");
        }
    }

    private boolean isCompleted(UUID userId, UUID questId) {
        return userQuestProgressRepository
                .findByUserIdAndQuestId(userId, questId)
                .map(p -> QuestStatus.COMPLETED.equals(p.getStatus()))
                .orElse(false);
    }
}
