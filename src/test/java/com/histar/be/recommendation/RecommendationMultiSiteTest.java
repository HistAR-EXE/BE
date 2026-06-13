package com.histar.be.recommendation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.recommendation.service.RecommendationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecommendationMultiSiteTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    @Autowired
    private UserDiscoveryRepository userDiscoveryRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private UUID userId;
    private UUID siteAId;
    private UUID siteBId;
    private Set<String> siteBKeys;

    @BeforeEach
    void setUp() {
        userId = profileRepository
                .save(Profile.builder()
                        .email("multi-site-" + UUID.randomUUID() + "@test.local")
                        .displayName("Tester")
                        .role("USER")
                        .totalPoints(0)
                        .level(1)
                        .createdAt(Instant.now())
                        .build())
                .getId();

        siteAId = locationRepository
                .save(Location.builder()
                        .name("Site A")
                        .latitude(11.0)
                        .longitude(106.0)
                        .city("A")
                        .createdAt(Instant.now())
                        .build())
                .getId();
        siteBId = locationRepository
                .save(Location.builder()
                        .name("Site B")
                        .latitude(12.0)
                        .longitude(107.0)
                        .city("B")
                        .createdAt(Instant.now())
                        .build())
                .getId();

        siteBKeys = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            String key = "scene:bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb" + i;
            siteBKeys.add(key);
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(siteBId)
                    .name("Site B POI " + i)
                    .mapXPct(BigDecimal.valueOf(10 + i))
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(i)
                    .build());
        }

        for (int i = 0; i < 8; i++) {
            String key = "scene:aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa" + i;
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(siteAId)
                    .name("Site A POI " + i)
                    .mapXPct(BigDecimal.TEN)
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(i)
                    .build());
            userDiscoveryRepository.save(UserDiscovery.builder()
                    .userId(userId)
                    .locationId(siteAId)
                    .discoveryKey(key)
                    .discoveredAt(Instant.now())
                    .build());
        }
    }

    @Test
    void recommendationsForSiteB_ignoreSiteADiscoveries() {
        var response = recommendationService.forUser(userId, siteBId, null);

        assertFalse(response.items().isEmpty());
        for (var item : response.items()) {
            assertTrue(siteBKeys.contains(item.unlockKey()), "Recommendation must be Site B POI: " + item.unlockKey());
            assertFalse(item.reason().contains("Site A"), "Reason must not reference Site A");
        }
    }
}
