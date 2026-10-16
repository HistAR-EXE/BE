package com.histar.be.discovery.explore;

import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.artifact.repository.UserArtifactRepository;
import com.histar.be.message.repository.MessageRepository;
import java.util.HashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class ExploreBadgeContextService {
    private final UserDiscoveryRepository discoveries;
    private final UserArtifactRepository artifacts;
    private final MessageRepository messages;

    public ExploreBadgeRules.Context context(UUID userId, int totalPoints) {
        var keys = new HashSet<String>();
        discoveries.findByUserId(userId).forEach(d -> {
            // Challenge counts belong to Cu Chi; feature experiences may come from the whole journey.
            if (ExploreChallengeCatalog.LOCATION.equals(d.getLocationId())
                    || d.getDiscoveryKey().startsWith("scene:") || d.getDiscoveryKey().startsWith("tour:")) {
                keys.add(d.getDiscoveryKey());
            }
        });
        return new ExploreBadgeRules.Context(totalPoints, keys,
                messages.countAssistantReplies(userId) > 0, !artifacts.findByUserId(userId).isEmpty());
    }
}
