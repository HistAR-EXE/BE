package com.histar.be.discovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.discovery.service.DiscoveryService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.math.BigDecimal;
import java.time.Instant;
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
class DiscoveryValidationTest {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    @Autowired
    private ProfileRepository profileRepository;

    private UUID userId;
    private UUID locationId;
    private static final String VALID_KEY = "photo:cua-ham";

    @BeforeEach
    void setUp() {
        locationId = UUID.randomUUID();
        discoveryPointRepository.save(DiscoveryPoint.builder()
                .locationId(locationId)
                .name("Cửa hầm")
                .mapXPct(BigDecimal.valueOf(50))
                .mapYPct(BigDecimal.valueOf(50))
                .unlockKey(VALID_KEY)
                .sortOrder(1)
                .build());
        Profile profile = profileRepository.save(Profile.builder()
                .email("discovery-val@test.local")
                .passwordHash("hash")
                .displayName("Tester")
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void record_rejectsUnknownUnlockKey() {
        assertThrows(
                BusinessRuleException.class,
                () -> discoveryService.record(userId, "invalid:key", locationId));
    }

    @Test
    void record_acceptsCatalogKey() {
        assertTrue(discoveryService.record(userId, VALID_KEY, locationId));
        assertFalse(discoveryService.record(userId, VALID_KEY, locationId));
    }
}
