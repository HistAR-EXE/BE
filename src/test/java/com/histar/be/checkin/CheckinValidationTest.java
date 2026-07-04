package com.histar.be.checkin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CheckinValidationTest {

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID userId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        Location location = locationRepository.save(Location.builder()
                .name("Checkin Validation Site")
                .latitude(11.143)
                .longitude(106.461)
                .city("TP.HCM")
                .createdAt(Instant.now())
                .build());
        locationId = location.getId();

        Profile profile = profileRepository.save(Profile.builder()
                .email("checkin-val-" + UUID.randomUUID() + "@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Checkin Tester")
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
        userId = profile.getId();
    }

    @Test
    void checkin_withinRadius_succeeds() {
        CheckinResultDto result = gamificationService.processCheckin(
                userId,
                locationId,
                11.143,
                106.461,
                "timelens:location:" + locationId);
        assertTrue(result.success());
    }

    @Test
    void checkin_tooFar_rejected() {
        assertThrows(
                BusinessRuleException.class,
                () -> gamificationService.processCheckin(
                        userId,
                        locationId,
                        21.0,
                        105.0,
                        "timelens:location:" + locationId));
    }

    @Test
    void checkin_qrLocationMismatch_rejected() {
        UUID other = UUID.randomUUID();
        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> gamificationService.processCheckin(
                        userId, locationId, 11.143, 106.461, "timelens:location:" + other));
        assertFalse(ex.getMessage().isBlank());
    }
}
