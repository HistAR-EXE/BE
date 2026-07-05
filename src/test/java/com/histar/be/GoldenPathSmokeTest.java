package com.histar.be;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.auth.service.AuthService;
import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.character.entity.CharacterEntity;
import com.histar.be.character.service.CharacterService;
import com.histar.be.config.ViralProperties;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import java.math.BigDecimal;
import java.util.List;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.hotspot.entity.Hotspot;
import com.histar.be.hotspot.service.HotspotService;
import com.histar.be.leaderboard.service.LeaderboardService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.service.LocationService;
import com.histar.be.media.service.MediaStorageService;
import com.histar.be.panorama.entity.Panorama;
import com.histar.be.panorama.service.PanoramaService;
import com.histar.be.photoframe.entity.PhotoFrame;
import com.histar.be.photoframe.service.PhotoFrameService;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.service.QuestProgressService;
import com.histar.be.share.dto.SharePrefillResponse;
import com.histar.be.usercreation.service.UserCreationAppService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GoldenPathSmokeTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CharacterService characterService;

    @Autowired
    private PanoramaService panoramaService;

    @Autowired
    private HotspotService hotspotService;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private QuestProgressService questProgressService;

    @Autowired
    private GamificationService gamificationService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PhotoFrameService photoFrameService;

    @Autowired
    private UserCreationAppService userCreationAppService;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private ViralProperties viralProperties;

    @Autowired
    private DiscoveryPointRepository discoveryPointRepository;

    @Test
    void goldenPath_week1ToWeek3_shouldRunWithoutBreak() {
        Location location = locationService.save(Location.builder()
                .name("Cu Chi")
                .description("Seed for smoke")
                .latitude(11.143)
                .longitude(106.461)
                .city("TP.HCM")
                .coverImage("https://example.com/cover.jpg")
                .createdAt(Instant.now())
                .build());

        CharacterEntity character = characterService.save(CharacterEntity.builder()
                .locationId(location.getId())
                .name("Chi Nam")
                .era("1968")
                .personaPrompt("test")
                .portraitUrl("https://example.com/chi-nam.jpg")
                .build());

        Panorama panorama = panoramaService.save(Panorama.builder()
                .locationId(location.getId())
                .imageUrl("https://example.com/pano.jpg")
                .title("Cu Chi Panorama")
                .build());

        hotspotService.save(Hotspot.builder()
                .panoramaId(panorama.getId())
                .yaw(12.5)
                .pitch(-3.2)
                .type("info")
                .contentRef("hut-1")
                .label("Tunnel entry")
                .build());

        for (String key : List.of("era:2026", "photo:cua-ham", "photo:gieng")) {
            discoveryPointRepository.save(DiscoveryPoint.builder()
                    .locationId(location.getId())
                    .name(key)
                    .mapXPct(BigDecimal.TEN)
                    .mapYPct(BigDecimal.TEN)
                    .unlockKey(key)
                    .sortOrder(1)
                    .build());
        }

        Quest quest = questRepository.save(Quest.builder()
                .locationId(location.getId())
                .title("Explore Cu Chi")
                .description("Check-in to complete")
                .story("Secret story")
                .pointsReward(100)
                .requiredOrder(1)
                .build());

        badgeRepository.save(Badge.builder()
                .name("Explorer badge")
                .description("Complete one quest")
                .conditionType("quest_complete")
                .conditionValue(1)
                .build());

        PhotoFrame frame = photoFrameService.save(PhotoFrame.builder()
                .name("Frame 1970")
                .imageUrl("https://example.com/frame.png")
                .era("1970")
                .sortOrder(1)
                .build());

        String email = "golden.path@test.local";
        String password = "Demo@2026";
        var register = authService.register(new RegisterRequest(email, password, "Golden User"));
        var login = authService.login(new LoginRequest(email, password));

        assertNotNull(register.userId());
        assertNotNull(login.token());
        assertEquals(register.userId(), login.userId());

        assertFalse(locationService.findAll().isEmpty());
        assertFalse(characterService.findByLocationId(location.getId()).isEmpty());
        assertFalse(panoramaService.findByLocationId(location.getId()).isEmpty());
        assertFalse(hotspotService.findByPanoramaId(panorama.getId()).isEmpty());
        assertFalse(questProgressService
                .listByLocation(location.getId(), PageRequest.of(0, 20))
                .isEmpty());

        questProgressService.startQuest(register.userId(), quest.getId());
        var checkin = gamificationService.processCheckin(
                register.userId(), location.getId(), 11.143, 106.461, "timelens:location:" + location.getId());
        assertTrue(checkin.success());
        assertFalse(checkin.questsCompleted().isEmpty());

        var profile = profileRepository.findById(register.userId()).orElseThrow();
        assertTrue(profile.getTotalPoints() >= 100);

        var upload = userCreationAppService.upload(
                register.userId(),
                frame.getId(),
                "story",
                new MockMultipartFile("file", "frame.jpg", "image/jpeg", "demo".getBytes()));
        assertNotNull(upload.id());

        var share = userCreationAppService.recordShare(register.userId(), upload.id());
        assertEquals(viralProperties.getShareBonusPoints(), share.bonusPointsAwarded());

        var leaderboard = leaderboardService.getLeaderboard("all", null, register.userId(), null);
        assertFalse(leaderboard.entries().isEmpty());
        assertTrue(leaderboard.entries().stream().anyMatch(e -> register.userId().equals(e.userId())));

        SharePrefillResponse prefill = new SharePrefillResponse(viralProperties.getShareCaption(), new String[] {"#TimeLens"});
        assertNotNull(prefill.caption());
        assertNotNull(character.getId());
    }

    @TestConfiguration
    @Profile("test")
    static class StubMediaStorageConfiguration {

        @Bean
        @Primary
        MediaStorageService mediaStorageService() {
            return (data, contentType, objectKey) -> "https://mock-storage.local/" + objectKey;
        }
    }
}
