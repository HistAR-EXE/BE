package com.histar.be.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.minigame.dto.MinigameResponse;
import com.histar.be.minigame.entity.StationMinigame;
import com.histar.be.minigame.repository.StationMinigameRepository;
import com.histar.be.minigame.service.MinigameService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Mini game của trạm chỉ mở sau khi quét QR check-in tại chính trạm đó. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StationMinigameLockTest {

    private static final String SITE = "lock-test-site";

    @Autowired private MinigameService minigameService;
    @Autowired private StationMinigameRepository minigameRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private CheckinRepository checkinRepository;

    private UUID userId;
    private UUID locationId;
    private UUID gameSt01;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        locationId = locationRepository
                .save(Location.builder()
                        .name("Di tích khoá mini game")
                        .siteCode(SITE)
                        .latitude(10.0)
                        .longitude(106.0)
                        .city("TP.HCM")
                        .createdAt(Instant.now())
                        .build())
                .getId();
        gameSt01 = game("ST01", "Đố nhanh ST01");
        game("ST02", "Đố nhanh ST02");
    }

    private UUID game(String stationCode, String title) {
        return minigameRepository
                .save(StationMinigame.builder()
                        .id(UUID.randomUUID())
                        .siteCode(SITE)
                        .stationCode(stationCode)
                        .gameType("QUIZ_TIMED")
                        .title(title)
                        .configJson("{\"questions\":[{\"prompt\":\"?\",\"options\":[\"a\"],\"correctIndex\":0}]}")
                        .sortOrder(1)
                        .build())
                .getId();
    }

    private void checkin(String stationCode) {
        checkinRepository.save(Checkin.builder()
                .userId(userId)
                .locationId(locationId)
                .stationCode(stationCode)
                .presenceMethod("qr")
                .createdAt(Instant.now())
                .build());
    }

    @Test
    void lockedForGuestAndForUserWhoHasNotScannedTheStation() {
        MinigameResponse guest = minigameService.listForStation(SITE, "ST01", null).get(0);
        assertFalse(guest.unlocked());
        assertEquals(MinigameService.LOCK_CHECKIN, guest.lockReason());
        assertTrue(guest.config().isEmpty(), "config phải rỗng khi còn khoá để không lộ đáp án");

        MinigameResponse notThereYet = minigameService.listForStation(SITE, "ST01", userId).get(0);
        assertFalse(notThereYet.unlocked());
        assertTrue(notThereYet.config().isEmpty());
    }

    @Test
    void scanningOneStationOpensOnlyThatStationGame() {
        checkin("st01");

        MinigameResponse open = minigameService.listForStation(SITE, "ST01", userId).get(0);
        assertTrue(open.unlocked(), "quét QR ST01 (chữ thường) phải mở game ST01");
        assertEquals(null, open.lockReason());
        assertFalse(open.config().isEmpty());

        List<MinigameResponse> other = minigameService.listForStation(SITE, "ST02", userId);
        assertFalse(other.get(0).unlocked(), "trạm khác vẫn khoá");
    }

    @Test
    void scoreIsRejectedUntilTheStationIsScanned() {
        assertThrows(BusinessRuleException.class, () -> minigameService.submit(userId, gameSt01, 90));

        checkin("ST01");
        assertEquals(90, minigameService.submit(userId, gameSt01, 90).score());
    }
}
