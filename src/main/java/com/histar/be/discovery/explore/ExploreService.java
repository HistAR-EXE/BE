package com.histar.be.discovery.explore;

import com.histar.be.badge.service.BadgeAwardService;
import com.histar.be.badge.user.repository.UserBadgeRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.dto.DiscoverySummaryResponse;
import com.histar.be.discovery.dto.RecordDiscoveryResponse;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.discovery.service.DiscoveryRewardService;
import com.histar.be.organization.service.TrialEntitlementGuard;
import com.histar.be.profile.dto.ProfileMeResponse;
import com.histar.be.profile.service.ProfileMeService;
import com.histar.be.profile.service.ProfilePointsService;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ExploreService {
    private final ProfilePointsService points;
    private final ProfileMeService profiles;
    private final DiscoveryService discoveries;
    private final UserDiscoveryRepository userDiscoveries;
    private final DiscoveryRewardService rewards;
    private final ExploreAttemptRepository attempts;
    private final ExploreBadgeContextService contexts;
    private final BadgeAwardService badgeAwards;
    private final UserBadgeRepository badges;
    private final TrialEntitlementGuard trial;

    public record AttemptRequest(@NotNull UUID locationId, @NotBlank @Size(max = 128) String key,
            @Min(0) @Max(3) Integer answer) {}
    public record BadgeProgress(String id, boolean earned, Instant earnedAt, int progressPercent, String progressText) {}
    public record Snapshot(ProfileMeResponse profile, DiscoverySummaryResponse discovery,
            int archiveXp, List<BadgeProgress> badges) {}
    public record AttemptResponse(boolean passed, boolean alreadyCompleted, int xpEarned, Snapshot snapshot) {}
    public record ExperienceRequest(@NotNull UUID locationId, @NotBlank @Size(max = 32) String type) {}

    /** A viewing receipt is durable but grants no XP and cannot complete a graded challenge. */
    @Transactional
    public Snapshot experience(UUID userId, ExperienceRequest request) {
        trial.assertStudentCanWrite(userId);
        points.lockAccount(userId);
        if (!ExploreChallengeCatalog.LOCATION.equals(request.locationId()) || !"diorama".equals(request.type())) {
            throw new BusinessRuleException("Trải nghiệm không hợp lệ tại địa điểm này.");
        }
        discoveries.record(userId, "feature:diorama", request.locationId());
        return snapshot(userId);
    }

    @Transactional
    public Snapshot snapshot(UUID userId) {
        var profile = points.lockAccount(userId);
        badgeAwards.evaluateAndAward(userId);
        var earned = new HashMap<UUID, Instant>();
        badges.findByUserId(userId).forEach(b -> earned.put(b.getBadgeId(), b.getEarnedAt()));
        var progress = ExploreBadgeRules.evaluate(contexts.context(userId,
                profile.getTotalPoints() == null ? 0 : profile.getTotalPoints()));
        var list = new ArrayList<BadgeProgress>();
        for (int i = 0; i < progress.size(); i++) {
            var p = progress.get(i);
            var at = earned.get(ExploreBadgeRules.databaseId(i));
            list.add(new BadgeProgress(p.id(), at != null, at, at != null ? 100 : p.percent(), p.text()));
        }
        var summary = discoveries.summary(userId, ExploreChallengeCatalog.LOCATION);
        int archiveXp = summary.keys().stream().mapToInt(ExploreChallengeCatalog::xp).sum();
        return new Snapshot(profiles.build(profile), summary, archiveXp, list);
    }

    @Transactional
    public AttemptResponse submit(UUID userId, AttemptRequest request) {
        trial.assertStudentCanWrite(userId);
        points.lockAccount(userId);
        if (!ExploreChallengeCatalog.LOCATION.equals(request.locationId())) {
            throw new BusinessRuleException("Thử thách không thuộc địa điểm này.");
        }
        String key = request.key();
        if (!ExploreChallengeCatalog.QUIZZES.containsKey(key) && !ExploreChallengeCatalog.CLUES.contains(key)
                && !ExploreChallengeCatalog.CONCLUSION.equals(key)) {
            throw new BusinessRuleException("Thử thách không hợp lệ.");
        }
        if (userDiscoveries.existsByUserIdAndLocationIdAndDiscoveryKey(userId, request.locationId(), key)) {
            return new AttemptResponse(true, true, 0, snapshot(userId));
        }
        if (attempts.countByUserIdAndSubmittedAtAfter(userId, Instant.now().minusSeconds(60)) >= 12) {
            throw new BusinessRuleException("Bạn thao tác quá nhanh. Vui lòng chờ một phút rồi thử lại.");
        }
        boolean passed;
        if (ExploreChallengeCatalog.QUIZZES.containsKey(key)) {
            passed = ExploreChallengeCatalog.grade(key, request.answer());
        } else {
            if (request.answer() != null) throw new BusinessRuleException("Hoạt động này không nhận đáp án trắc nghiệm.");
            passed = true;
            if (ExploreChallengeCatalog.CONCLUSION.equals(key)) {
                for (String clue : ExploreChallengeCatalog.CLUES) {
                    if (!userDiscoveries.existsByUserIdAndLocationIdAndDiscoveryKey(userId, request.locationId(), clue)) {
                        throw new BusinessRuleException("Cần ghi nhận đủ bốn chứng cứ trước khi kết luận.");
                    }
                }
            }
        }
        attempts.save(ExploreAttempt.builder().userId(userId).locationId(request.locationId())
                .challengeKey(key).answer(request.answer()).passed(passed).submittedAt(Instant.now()).build());
        RecordDiscoveryResponse reward = passed ? rewards.record(userId, request.locationId(), key, "explore") : null;
        if (passed && (reward == null || !reward.recorded())) {
            throw new BusinessRuleException("Chưa thể ghi nhận thử thách. Vui lòng thử lại sau.");
        }
        return new AttemptResponse(passed, false, reward != null ? reward.xpEarned() : 0, snapshot(userId));
    }
}
