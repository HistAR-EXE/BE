package com.histar.be;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.analytics.service.AdminAnalyticsService;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.seed.SeedConsistencyValidator;
import com.histar.be.support.CuChiTestFixture;
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
class DemoReadinessTest {

    @Autowired
    private CuChiTestFixture fixture;

    @Autowired
    private SeedConsistencyValidator seedConsistencyValidator;

    @Autowired
    private AdminAnalyticsService adminAnalyticsService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private ArtifactRepository artifactRepository;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    private UUID locationId;

    @BeforeEach
    void setUp() {
        locationId = fixture.loadValidFixture().locationId();
    }

    @Test
    void demoReadiness_minimumDataAndQueries() {
        assertTrue(locationRepository.count() >= 1);
        assertTrue(questRepository.count() >= 1);
        assertTrue(badgeRepository.count() >= 1);
        assertTrue(artifactRepository.count() >= 1);
        assertTrue(discoveryPointRepository.count() >= 1);

        assertDoesNotThrow(() -> seedConsistencyValidator.validateAll());
        assertDoesNotThrow(() -> adminAnalyticsService.overview(locationId));
    }
}
