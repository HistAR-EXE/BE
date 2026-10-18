package com.histar.be.leaderboard;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.histar.be.config.GamificationProperties;
import com.histar.be.config.ViralProperties;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository;
import com.histar.be.leaderboard.repository.LeaderboardReadRepository.RankedRow;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {
    @Mock ProfileRepository profiles;
    @Mock StudyGroupMemberRepository members;
    @Mock LeaderboardReadRepository ranking;
    LeaderboardServiceImpl service;
    UUID viewer = UUID.randomUUID();
    @BeforeEach void setup() {
        var props = new ViralProperties(); props.setLeaderboardLimit(3);
        service = new LeaderboardServiceImpl(profiles, members, props, new GamificationProperties(), ranking);
    }
    private Profile account(String tier) {
        return Profile.builder().id(viewer).displayName("Explorer").role("USER").tier(tier).totalPoints(120).build();
    }
    private RankedRow row(UUID id, int xp, int rank, long ordinal) {
        return new RankedRow(id, "Explorer", null, xp, rank, ordinal, 12, 2);
    }
    @Test void freeAndPremiumHaveIdenticalRanksAndNoPaywall() {
        var rows = List.of(row(viewer, 120, 1, 1));
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")), Optional.of(account("PREMIUM")));
        when(ranking.read(viewer, null, null, null, 10)).thenReturn(rows);
        var free = service.getLeaderboard("all", null, viewer, null);
        var premium = service.getLeaderboard("all", null, viewer, null);
        assertThat(free.entries()).isEqualTo(premium.entries());
        assertThat(free.viewerRankLocked()).isFalse();
        assertThat(free.viewer().currentUser()).isTrue();
    }
    @Test void anonymousCannotReadCommunityHonors() {
        assertThatThrownBy(() -> service.getLeaderboard("all", null, null, null)).hasMessageContaining("đăng nhập");
        verifyNoInteractions(profiles, ranking);
    }
    @Test void weekScopeIsNoLongerAvailable() {
        assertThatThrownBy(() -> service.getLeaderboard("week", null, viewer, null)).hasMessageContaining("toàn hành trình");
        verifyNoInteractions(ranking);
    }
    @Test void foreignGroupCannotBeRead() {
        assertThatThrownBy(() -> service.getLeaderboard("all", null, viewer, UUID.randomUUID())).hasMessageContaining("thành viên");
        verifyNoInteractions(ranking);
    }
    @Test void viewerOutsideTopGetsRealRankAndNeighbors() {
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")));
        when(ranking.read(viewer, null, null, null, 10)).thenReturn(List.of(row(UUID.randomUUID(), 900, 1, 1),
                row(UUID.randomUUID(), 145, 11, 11), row(viewer, 120, 12, 12)));
        var result = service.getLeaderboard("all", null, viewer, null);
        assertThat(result.entries()).hasSize(1);
        assertThat(result.viewer().rank()).isEqualTo(12);
        assertThat(result.nearby()).hasSize(2);
        assertThat(result.totalParticipants()).isEqualTo(12);
        verify(profiles, never()).save(any());
    }
    @Test void zeroXpHasNoFakeRank() {
        var profile = account("FREE"); profile.setTotalPoints(0);
        when(profiles.findById(viewer)).thenReturn(Optional.of(profile));
        when(ranking.read(viewer, null, null, null, 10)).thenReturn(List.of());
        var result = service.getLeaderboard("all", null, viewer, null);
        assertThat(result.viewer().rank()).isNull();
        assertThat(result.viewer().totalPoints()).isZero();
        assertThat(result.totalParticipants()).isZero();
    }
    @Test void schoolScopeUsesOnlyViewerOrganization() {
        var profile = account("FREE"); var org = UUID.randomUUID(); profile.setOrgId(org);
        when(profiles.findById(viewer)).thenReturn(Optional.of(profile));
        when(ranking.read(viewer, null, org, null, 10)).thenReturn(List.of(row(viewer, 120, 1, 1)));
        assertThat(service.getLeaderboard("school", null, viewer, null).scope()).isEqualTo("school");
        verify(ranking).read(viewer, null, org, null, 10);
    }
    @Test void memberCanReadTheirGroupWithoutPremium() {
        UUID group = UUID.randomUUID();
        when(members.findByGroupIdAndUserId(group, viewer)).thenReturn(Optional.of(new StudyGroupMember()));
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")));
        when(ranking.read(viewer, null, null, group, 10)).thenReturn(List.of(row(viewer, 120, 1, 1)));
        assertThat(service.getLeaderboard("all", null, viewer, group).viewer().rank()).isEqualTo(1);
    }
    @Test void schoolWithoutOrganizationIsRejected() {
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")));
        assertThatThrownBy(() -> service.getLeaderboard("school", null, viewer, null)).hasMessageContaining("trường");
        verifyNoInteractions(ranking);
    }
    @Test void repeatedReadsDoNotReuseAnotherViewersHighlight() {
        UUID second = UUID.randomUUID();
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")));
        when(profiles.findById(second)).thenReturn(Optional.of(Profile.builder().id(second).totalPoints(90).build()));
        var rows = List.of(row(viewer, 120, 1, 1), row(second, 90, 2, 2));
        when(ranking.read(any(), isNull(), isNull(), isNull(), eq(10))).thenReturn(rows);
        assertThat(service.getLeaderboard("all", null, viewer, null).entries().get(0).currentUser()).isTrue();
        var other = service.getLeaderboard("all", null, second, null);
        assertThat(other.entries().get(0).currentUser()).isFalse();
        assertThat(other.entries().get(1).currentUser()).isTrue();
    }
    @Test void tenDisplayRowsRetainCompetitionTiesAndReportOmittedPeers() {
        when(profiles.findById(viewer)).thenReturn(Optional.of(account("FREE")));
        var rows = new ArrayList<RankedRow>();
        for(int i=1;i<=12;i++) rows.add(new RankedRow(i==12?viewer:UUID.randomUUID(), "Explorer",null,
            i<9?1000-i:100,i<9?i:9,i,12,12-i,i<9?1:4));
        when(ranking.read(viewer,null,null,null,10)).thenReturn(rows);
        var result=service.getLeaderboard("all",null,viewer,null);
        assertThat(result.entries()).hasSize(10);
        assertThat(result.entries().get(8).rank()).isEqualTo(9);
        assertThat(result.entries().get(9).rank()).isEqualTo(9);
        assertThat(result.displayLimit()).isEqualTo(10);
        assertThat(result.cutoffXp()).isEqualTo(100);
        assertThat(result.omittedTiedAtCutoff()).isEqualTo(2);
        assertThat(result.viewer().rank()).isEqualTo(9);
        assertThat(result.nearby()).hasSize(3);
    }
}
