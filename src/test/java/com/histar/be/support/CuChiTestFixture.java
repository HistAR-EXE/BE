package com.histar.be.support;

import com.histar.be.artifact.entity.Artifact;
import com.histar.be.artifact.repository.ArtifactRepository;
import com.histar.be.badge.entity.Badge;
import com.histar.be.badge.repository.BadgeRepository;
import com.histar.be.discovery.bridge.entity.DiscoveryArtifactLink;
import com.histar.be.discovery.bridge.repository.DiscoveryArtifactLinkRepository;
import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CuChiTestFixture {

    public static final String KEY_A = "scene:golden-path-a";
    public static final String KEY_B = "scene:golden-path-b";
    public static final String ARTIFACT_KEY = "artifact:golden-linked";

    private final LocationRepository locationRepository;
    private final DiscoveryPointRepository discoveryPointRepository;
    private final QuestRepository questRepository;
    private final BadgeRepository badgeRepository;
    private final ArtifactRepository artifactRepository;
    private final DiscoveryArtifactLinkRepository discoveryArtifactLinkRepository;

    public record CuChiFixtureData(UUID locationId, UUID questId, int questPointsReward) {}

    public CuChiFixtureData loadValidFixture() {
        Location location = locationRepository.save(Location.builder()
                .name("Củ Chi Golden Path")
                .latitude(11.143)
                .longitude(106.461)
                .city("TP.HCM")
                .createdAt(Instant.now())
                .build());
        UUID locationId = location.getId();

        discoveryPointRepository.save(DiscoveryPoint.builder()
                .locationId(locationId)
                .name("Bếp Hoàng Cầm")
                .mapXPct(BigDecimal.TEN)
                .mapYPct(BigDecimal.TEN)
                .unlockKey(KEY_A)
                .sortOrder(1)
                .build());
        discoveryPointRepository.save(DiscoveryPoint.builder()
                .locationId(locationId)
                .name("Phòng họp")
                .mapXPct(BigDecimal.valueOf(20))
                .mapYPct(BigDecimal.valueOf(20))
                .unlockKey(KEY_B)
                .sortOrder(2)
                .build());

        Quest quest = questRepository.save(Quest.builder()
                .locationId(locationId)
                .title("Golden Path Quest")
                .description("Discover and check in")
                .story("Secret")
                .pointsReward(150)
                .stepDiscoveryKeys(KEY_A + "," + KEY_B)
                .requiredOrder(1)
                .build());

        badgeRepository.save(Badge.builder()
                .name("Explorer")
                .conditionType("quest_complete")
                .conditionValue(1)
                .build());

        Artifact artifact = artifactRepository.save(Artifact.builder()
                .locationId(locationId)
                .name("Linked Artifact")
                .unlockKey(ARTIFACT_KEY)
                .sortOrder(1)
                .build());

        discoveryArtifactLinkRepository.save(DiscoveryArtifactLink.builder()
                .discoveryUnlockKey(KEY_A)
                .artifactUnlockKey(ARTIFACT_KEY)
                .locationId(locationId)
                .build());

        return new CuChiFixtureData(locationId, quest.getId(), quest.getPointsReward());
    }
}
