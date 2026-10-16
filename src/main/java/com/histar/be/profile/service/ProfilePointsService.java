package com.histar.be.profile.service;

import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.profile.repository.XpLedgerRepository;
import com.histar.be.profile.entity.XpLedgerEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfilePointsService {

    public static final int XP_DISCOVERY = 10;
    public static final int XP_ARTIFACT_UNLOCK = 5;
    public static final int XP_CHECKIN = 30;
    /** First check-in at a station within a site (ST01–ST06). */
    public static final int XP_STATION = 15;

    private final ProfileRepository profileRepository;
    private final GamificationProperties gamificationProperties;
    private final XpLedgerRepository ledger;
    private final EntityManager entityManager;

    @Transactional
    public Profile lockAccount(UUID userId) {
        Profile profile = profileRepository.findById(userId).orElseThrow();
        // A managed entity may have been read before another transaction committed.
        // Refresh on the first lock, never after this transaction has started awarding.
        if (entityManager.getLockMode(profile) != LockModeType.PESSIMISTIC_WRITE) {
            entityManager.flush();
            entityManager.refresh(profile, LockModeType.PESSIMISTIC_WRITE);
        }
        return profile;
    }

    @Transactional
    public int award(UUID userId, int points) {
        return awardOnce(userId, "event:" + UUID.randomUUID(), points);
    }

    @Transactional
    public int awardOnce(UUID userId, String eventKey, int points) {
        if (points <= 0) {
            return 0;
        }
        Profile profile = lockAccount(userId);
        if (ledger.existsByUserIdAndEventKey(userId, eventKey)) return 0;
        int current = profile.getTotalPoints() == null ? 0 : profile.getTotalPoints();
        // Preserve historical totals, without pretending that old client data was verified.
        if (!ledger.existsByUserIdAndEventKey(userId, "baseline:v1")) {
            ledger.save(XpLedgerEntry.builder().userId(userId).eventKey("baseline:v1")
                    .points(current).awardedAt(Instant.now()).build());
        }
        int next = Math.addExact(current, points);
        profile.setTotalPoints(next);
        profile.setLevel(LevelCalculator.levelFromPoints(next, gamificationProperties.parseLevelThresholds()));
        profileRepository.save(profile);
        ledger.save(XpLedgerEntry.builder().userId(userId).eventKey(eventKey)
                .points(points).awardedAt(Instant.now()).build());
        return points;
    }

    /**
     * Tính điểm XP tương ứng với từng loại hoạt động khám phá/giải mã trên ExplorePage.
     */
    public int resolveXpForDiscoveryKey(String unlockKey) {
        int archiveXp = com.histar.be.discovery.explore.ExploreChallengeCatalog.xp(unlockKey);
        if (archiveXp > 0) return archiveXp;
        if (unlockKey == null) {
            return XP_DISCOVERY;
        }
        return switch (unlockKey) {
            case "depth:surface", "depth:layer1", "depth:layer2", "depth:layer3", "feature:diorama" -> 0;
            default -> XP_DISCOVERY;
        };
    }
}
