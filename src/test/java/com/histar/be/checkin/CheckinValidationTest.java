package com.histar.be.checkin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.checkin.dto.CheckinRequest;
import com.histar.be.checkin.presence.PresenceInput;
import com.histar.be.checkin.presence.PresenceScore;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.checkin.service.PresenceCheckinService;
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
    private PresenceCheckinService presenceCheckinService;

    @Autowired
    private CheckinRepository checkinRepository;

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

    @Test
    void checkin_legacyGps_storesGpsPresence() {
        CheckinResultDto result = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, "timelens:location:" + locationId);
        assertEquals(PresenceScore.GPS_POINTS, result.presenceScore());
        assertEquals("GPS", result.presenceMethod());
    }

    @Test
    void checkin_verifiedStationQr_withGps_scoresQrPlusGps() {
        CheckinResultDto result = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, null, new PresenceInput("ST-01", true, null));
        assertEquals(PresenceScore.QR_POINTS + PresenceScore.GPS_POINTS, result.presenceScore());
        assertEquals("QR", result.presenceMethod());
    }

    @Test
    void checkin_verifiedStationQr_withoutGps_succeedsWithQrScore() {
        CheckinResultDto result = gamificationService.processCheckin(
                userId, locationId, null, null, null, new PresenceInput("ST-01", true, null));
        assertTrue(result.success());
        assertEquals(PresenceScore.QR_POINTS, result.presenceScore());
    }

    @Test
    void checkin_sequentialStation_addsSequenceBonus() {
        gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, null, new PresenceInput("ST-01", true, null));
        CheckinResultDto second = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, null, new PresenceInput("ST-02", true, null));
        assertEquals(100, second.presenceScore());
    }

    @Test
    void checkin_noQrAndTooFar_rejected() {
        assertThrows(
                BusinessRuleException.class,
                () -> gamificationService.processCheckin(
                        userId, locationId, 21.0, 105.0, null, new PresenceInput("ST-01", false, null)));
    }

    @Test
    void checkin_clientUuid_isIdempotent() {
        UUID clientUuid = UUID.randomUUID();
        PresenceInput input = new PresenceInput("ST-01", true, clientUuid);
        CheckinResultDto first = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, null, input);
        long countAfterFirst = checkinRepository.countByUserId(userId);
        CheckinResultDto replay = gamificationService.processCheckin(
                userId, locationId, 11.143, 106.461, null, input);
        assertEquals(countAfterFirst, checkinRepository.countByUserId(userId));
        assertTrue(replay.success());
        assertEquals(first.presenceScore(), replay.presenceScore());
        assertEquals(0, replay.xpEarned());
    }

    @Test
    void presenceService_unsignedStationQr_acceptedInDemoMode() {
        var response = presenceCheckinService.checkin(
                userId,
                new CheckinRequest(locationId, 11.143, 106.461, null, "ST-01", "ST-01:1:", "QR", null));
        assertEquals("QR", response.presenceMethod());
    }

    @Test
    void presenceService_requiresQrCodeOrPayload() {
        assertThrows(
                BusinessRuleException.class,
                () -> presenceCheckinService.checkin(
                        userId, new CheckinRequest(locationId, 11.143, 106.461, null, null, null, null, null)));
    }
}
