package com.histar.be.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.config.ViralProperties;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private StudyGroupMemberRepository studyGroupMemberRepository;

    @Mock
    private UsageQuotaService usageQuotaService;

    private LeaderboardServiceImpl leaderboardService;

    @BeforeEach
    void setUp() {
        ViralProperties props = new ViralProperties();
        props.setLeaderboardLimit(10);
        props.setLeaderboardCacheSeconds(60);
        leaderboardService =
                new LeaderboardServiceImpl(profileRepository, studyGroupMemberRepository, props, usageQuotaService);
    }

    @Test
    void allScope_ranksByPoints() {
        UUID user1 = UUID.randomUUID();
        when(usageQuotaService.hasPremiumEntitlement(user1)).thenReturn(true);
        when(profileRepository.findLeaderboard(isNull(), any(Pageable.class)))
                .thenReturn(List.of(
                        Profile.builder().id(user1).displayName("A").totalPoints(500).build(),
                        Profile.builder().id(UUID.randomUUID()).displayName("B").totalPoints(100).build()));

        var result = leaderboardService.getLeaderboard("all", null, user1, null);
        assertEquals(2, result.entries().size());
        assertEquals(1, result.entries().get(0).rank());
        assertEquals(true, result.entries().get(0).currentUser());
    }

    @Test
    void allScope_allowsArchivedOrgMembersReadOnlyAccess() {
        UUID userId = UUID.randomUUID();
        when(usageQuotaService.hasPremiumEntitlement(userId)).thenReturn(false);
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).orgId(UUID.randomUUID()).build()));
        when(profileRepository.findLeaderboard(isNull(), any(Pageable.class)))
                .thenReturn(List.of(Profile.builder().id(userId).displayName("Archived").totalPoints(120).build()));

        var result = leaderboardService.getLeaderboard("all", null, userId, null);

        assertEquals(1, result.entries().size());
        assertEquals(true, result.entries().get(0).currentUser());
        assertEquals(false, result.viewerRankLocked());
    }

    @Test
    void allScope_freeUsersGetTruncatedPreviewWithoutRankHighlight() {
        UUID userId = UUID.randomUUID();
        when(usageQuotaService.hasPremiumEntitlement(userId)).thenReturn(false);
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).build()));
        when(profileRepository.findLeaderboard(isNull(), any(Pageable.class)))
                .thenReturn(List.of(
                        Profile.builder().id(userId).displayName("Free").totalPoints(900).build(),
                        Profile.builder().id(UUID.randomUUID()).displayName("B").totalPoints(800).build()));

        var result = leaderboardService.getLeaderboard("all", null, userId, null);

        assertEquals(2, result.entries().size());
        assertEquals(true, result.viewerRankLocked());
        assertEquals(false, result.entries().stream().anyMatch(e -> e.currentUser()));
    }
}
