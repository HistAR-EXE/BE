package com.histar.be.leaderboard.service.impl;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.ViralProperties;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.leaderboard.dto.LeaderboardEntryResponse;
import com.histar.be.leaderboard.dto.LeaderboardResponse;
import com.histar.be.leaderboard.service.LeaderboardService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeaderboardServiceImpl implements LeaderboardService {

    private final ProfileRepository profileRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final ViralProperties viralProperties;
    private final Map<String, CachedLeaderboard> cache = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(String scope, String city, UUID currentUserId, UUID groupId) {
        if (groupId != null) {
            return getGroupLeaderboard(groupId, currentUserId);
        }
        String normalizedScope = scope == null ? "all" : scope.toLowerCase();
        if (!normalizedScope.equals("all") && !normalizedScope.equals("city") && !normalizedScope.equals("week")) {
            throw new BusinessRuleException("scope phải là all, city hoặc week");
        }
        if (normalizedScope.equals("city") && (city == null || city.isBlank())) {
            throw new BusinessRuleException("scope=city cần tham số city");
        }

        String cacheKey = normalizedScope + "|" + (city == null ? "" : city);
        CachedLeaderboard cached = cache.get(cacheKey);
        long now = System.currentTimeMillis();
        if (cached != null && now - cached.cachedAtMs() < viralProperties.getLeaderboardCacheSeconds() * 1000L) {
            return applyCurrentUserHighlight(cached.response(), currentUserId);
        }

        List<Profile> profiles = loadProfiles(normalizedScope, city);
        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        int rank = 1;
        for (Profile profile : profiles) {
            if (profile.getTotalPoints() == null) {
                continue;
            }
            entries.add(new LeaderboardEntryResponse(
                    rank++,
                    profile.getId(),
                    profile.getDisplayName() == null ? "Explorer" : profile.getDisplayName(),
                    profile.getAvatarUrl(),
                    profile.getTotalPoints(),
                    false));
        }

        LeaderboardResponse response = new LeaderboardResponse(normalizedScope, city, entries);
        cache.put(cacheKey, new CachedLeaderboard(response, now));
        return applyCurrentUserHighlight(response, currentUserId);
    }

    private LeaderboardResponse getGroupLeaderboard(UUID groupId, UUID currentUserId) {
        List<UUID> memberIds = studyGroupMemberRepository.findByGroupId(groupId).stream()
                .map(m -> m.getUserId())
                .toList();
        if (memberIds.isEmpty()) {
            return new LeaderboardResponse("group", null, List.of());
        }
        List<Profile> profiles = profileRepository.findAllById(memberIds).stream()
                .filter(p -> p.getTotalPoints() != null)
                .sorted((a, b) -> Integer.compare(b.getTotalPoints(), a.getTotalPoints()))
                .limit(viralProperties.getLeaderboardLimit())
                .toList();
        List<LeaderboardEntryResponse> entries = new ArrayList<>();
        int rank = 1;
        for (Profile profile : profiles) {
            entries.add(new LeaderboardEntryResponse(
                    rank++,
                    profile.getId(),
                    profile.getDisplayName() == null ? "Explorer" : profile.getDisplayName(),
                    profile.getAvatarUrl(),
                    profile.getTotalPoints(),
                    false));
        }
        return applyCurrentUserHighlight(new LeaderboardResponse("group", null, entries), currentUserId);
    }

    private List<Profile> loadProfiles(String scope, String city) {
        int limit = viralProperties.getLeaderboardLimit();
        PageRequest page = PageRequest.of(0, limit);
        Instant weekStart = "week".equals(scope) ? Instant.now().minus(7, ChronoUnit.DAYS) : null;
        String cityParam = switch (scope) {
            case "city" -> city;
            case "week" -> (city != null && !city.isBlank() ? city : null);
            default -> null;
        };
        return "week".equals(scope)
                ? profileRepository.findLeaderboardSince(cityParam, weekStart, page)
                : profileRepository.findLeaderboard(cityParam, page);
    }

    private LeaderboardResponse applyCurrentUserHighlight(LeaderboardResponse source, UUID currentUserId) {
        if (currentUserId == null) {
            return source;
        }
        List<LeaderboardEntryResponse> entries = source.entries().stream()
                .map(e -> new LeaderboardEntryResponse(
                        e.rank(),
                        e.userId(),
                        e.displayName(),
                        e.avatarUrl(),
                        e.totalPoints(),
                        currentUserId.equals(e.userId())))
                .toList();
        return new LeaderboardResponse(source.scope(), source.city(), entries);
    }

    private record CachedLeaderboard(LeaderboardResponse response, long cachedAtMs) {}
}
