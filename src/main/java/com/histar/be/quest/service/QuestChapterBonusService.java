package com.histar.be.quest.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.profile.service.ProfilePointsService;
import com.histar.be.quest.dto.ChapterBonusResponse;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.entity.QuestStep;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestChapterBonusService {

    public static final String SA_BAN_STEP = "tour:sa-ban";
    public static final String SA_BAN_BONUS = "bonus:tour:sa-ban";

    private final UserDiscoveryRepository userDiscoveryRepository;
    private final QuestRepository questRepository;
    private final ProfilePointsService profilePointsService;

    @Transactional
    public ChapterBonusResponse claim(UUID userId, UUID locationId, String unlockKey) {
        profilePointsService.lockAccount(userId);
        if (!SA_BAN_STEP.equals(unlockKey)) {
            throw new BusinessRuleException("Chương này không có thưởng đố.");
        }
        if (!userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(userId, locationId, SA_BAN_STEP)) {
            throw new BusinessRuleException("Chưa nộp mã mảnh sa bàn.");
        }
        if (userDiscoveryRepository.existsByUserIdAndLocationIdAndDiscoveryKey(userId, locationId, SA_BAN_BONUS)) {
            return new ChapterBonusResponse(0, true);
        }
        try {
            userDiscoveryRepository.saveAndFlush(UserDiscovery.builder()
                    .userId(userId)
                    .locationId(locationId)
                    .discoveryKey(SA_BAN_BONUS)
                    .discoveredAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException already) {
            return new ChapterBonusResponse(0, true);
        }
        int awarded = profilePointsService.awardOnce(userId, "chapter:" + locationId + ":" + unlockKey, chapterXp(locationId));
        return new ChapterBonusResponse(awarded, false);
    }

    private int chapterXp(UUID locationId) {
        for (Quest quest : questRepository.findByLocationId(locationId)) {
            if (quest.getSteps() == null) {
                continue;
            }
            for (QuestStep step : quest.getSteps()) {
                if (SA_BAN_STEP.equals(step.getUnlockKey()) && step.getXpPartial() != null && step.getXpPartial() > 0) {
                    return step.getXpPartial();
                }
            }
        }
        return 50;
    }
}
