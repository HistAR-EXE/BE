package com.histar.be.discovery.dto;

import com.histar.be.gamification.dto.QuestProgressSnapshotDto;
import com.histar.be.gamification.dto.UnlockedArtifactDto;
import com.histar.be.location.dto.LocationResponse;
import java.util.List;

public record RecordDiscoveryResponse(
        boolean recorded,
        int xpEarned,
        List<UnlockedArtifactDto> newArtifacts,
        QuestProgressSnapshotDto questProgress,
        List<LocationResponse> newlyUnlockedLocations) {}