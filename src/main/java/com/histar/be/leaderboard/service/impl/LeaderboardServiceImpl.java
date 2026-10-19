package com.histar.be.leaderboard.service.impl;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.gamification.LevelCalculator;
import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.leaderboard.dto.LeaderboardEntryResponse;
import com.histar.be.leaderboard.dto.LeaderboardResponse;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository.RankedRow;
import com.histar.be.leaderboard.service.LeaderboardService;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {
    private final ProfileRepository profiles;
    private final StudyGroupMemberRepository members;
    private final ViralProperties properties;
    private final GamificationProperties gamification;
    private final LeaderboardReadRepository ranking;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LeaderboardResponse getLeaderboard(String scope, String city, UUID currentUserId, UUID groupId) {
        var access = resolveAccess(scope, city, currentUserId, groupId);
        var profile = access.profile();
        String normalized = access.scope();
        String cityFilter = access.city();
        UUID orgId = access.orgId();
        int limit = 10;
        List<RankedRow> rows = ranking.read(currentUserId, cityFilter, orgId, groupId, limit);
        RankedRow me = rows.stream().filter(row -> row.userId().equals(currentUserId)).findFirst().orElse(null);
        var entries = rows.stream().filter(row -> row.ordinal() <= limit).map(row -> entry(row, currentUserId)).toList();
        var nearby = me == null ? List.<LeaderboardEntryResponse>of() : rows.stream()
                .filter(row -> Math.abs(row.ordinal() - me.ordinal()) <= 2).map(row -> entry(row, currentUserId)).toList();
        int points = Math.max(0, profile.getTotalPoints() == null ? 0 : profile.getTotalPoints());
        String name = profile.getDisplayName() == null || profile.getDisplayName().isBlank() ? "Nhà thám hiểm" : profile.getDisplayName();
        var viewer = me != null ? entry(me, currentUserId) : new LeaderboardEntryResponse(null, currentUserId,
                name, profile.getAvatarUrl(), points, true, level(points), ranking.badgeCount(currentUserId));
        long count = rows.isEmpty() ? 0 : rows.get(0).participants();
        RankedRow cutoff = rows.stream().filter(row -> row.ordinal() == limit).findFirst().orElse(null);
        Integer cutoffXp = cutoff == null ? null : cutoff.points();
        long omitted = cutoff == null ? 0 : Math.max(0, cutoff.xpPeers() - rows.stream()
                .filter(row -> row.ordinal() <= limit && row.points() == cutoff.points()).count());
        return new LeaderboardResponse(normalized, cityFilter, entries, false, viewer, nearby, count, Instant.now(), limit, cutoffXp, omitted);
    }

    public record ScopeAccess(String scope, String city, UUID orgId, com.histar.be.profile.entity.Profile profile) {}

    /** Shared authorization only: optional stories never invoke the ranking query. */
    public ScopeAccess resolveAccess(String scope, String city, UUID currentUserId, UUID groupId) {
        if (currentUserId == null) throw new AuthException("Vui lòng đăng nhập để xem bảng vinh danh.");
        String normalized = groupId != null ? "group" : scope == null ? "all" : scope.trim().toLowerCase(Locale.ROOT);
        if (!List.of("all", "city", "school", "group").contains(normalized)) {
            throw new BusinessRuleException("Bảng vinh danh chỉ ghi nhận thành tích toàn hành trình; phạm vi hợp lệ: all, city, school.");
        }
        if ("group".equals(normalized) && (groupId == null || members.findByGroupIdAndUserId(groupId, currentUserId).isEmpty())) {
            throw new AccessDeniedException("Chỉ thành viên được xem bảng vinh danh của nhóm.");
        }
        var profile = profiles.findById(currentUserId)
                .orElseThrow(() -> new AuthException("Vui lòng đăng nhập lại để xem bảng vinh danh."));
        UUID orgId = "school".equals(normalized) ? profile.getOrgId() : null;
        if ("school".equals(normalized) && orgId == null) throw new BusinessRuleException("Tài khoản chưa thuộc trường nào.");
        String cityFilter = "city".equals(normalized) && city != null ? city.trim() : null;
        if ("city".equals(normalized) && (cityFilter == null || cityFilter.isBlank() || cityFilter.length() > 100)) {
            throw new BusinessRuleException("Vui lòng chọn tên tỉnh/thành hợp lệ.");
        }
        return new ScopeAccess(normalized, cityFilter, orgId, profile);
    }

    private LeaderboardEntryResponse entry(RankedRow row, UUID viewer) {
        return new LeaderboardEntryResponse(row.rank(), row.userId(), row.displayName(), row.avatarUrl(),
                row.points(), row.userId().equals(viewer), level(row.points()), row.badgeCount());
    }

    private int level(int points) {
        return LevelCalculator.info(points, gamification.parseLevelThresholds(), gamification.parseLevelNames()).level();
    }
}
