package com.histar.be.leaderboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.histar.be.config.ViralProperties;
import com.histar.be.leaderboard.service.impl.LeaderboardServiceImpl;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.List;
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

    private LeaderboardServiceImpl leaderboardService;

    @BeforeEach
    void setUp() {
        ViralProperties props = new ViralProperties();
        props.setLeaderboardLimit(10);
        props.setLeaderboardCacheSeconds(60);
        leaderboardService = new LeaderboardServiceImpl(profileRepository, props);
    }

    @Test
    void allScope_ranksByPoints() {
        UUID user1 = UUID.randomUUID();
        when(profileRepository.findLeaderboard(isNull(), any(Pageable.class)))
                .thenReturn(List.of(
                        Profile.builder().id(user1).displayName("A").totalPoints(500).build(),
                        Profile.builder().id(UUID.randomUUID()).displayName("B").totalPoints(100).build()));

        var result = leaderboardService.getLeaderboard("all", null, user1);
        assertEquals(2, result.entries().size());
        assertEquals(1, result.entries().get(0).rank());
        assertEquals(true, result.entries().get(0).currentUser());
    }
}
