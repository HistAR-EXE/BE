package com.histar.be.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.recommendation.service.RecommendationService;
import com.histar.be.support.CuChiTestFixture;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationControllerTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private CuChiTestFixture cuChiTestFixture;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void recommendations_forUser_returnsItemsList() {
        var fixture = cuChiTestFixture.loadValidFixture();
        Profile profile = profileRepository.save(Profile.builder()
                .email("rec-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Rec Tester")
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());

        var response = recommendationService.forUser(profile.getId(), fixture.locationId(), null);
        assertThat(response.items()).isNotNull();
    }
}
