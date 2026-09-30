package com.histar.be.squad.sync;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SquadLiveStateService {

    private final Map<UUID, Map<UUID, SquadMemberLiveState>> bySquad = new ConcurrentHashMap<>();

    public SquadMemberLiveState get(UUID squadId, UUID userId) {
        Map<UUID, SquadMemberLiveState> room = bySquad.get(squadId);
        if (room == null) {
            return SquadMemberLiveState.empty(userId);
        }
        return room.getOrDefault(userId, SquadMemberLiveState.empty(userId));
    }

    public Map<UUID, SquadMemberLiveState> snapshot(UUID squadId) {
        Map<UUID, SquadMemberLiveState> room = bySquad.get(squadId);
        if (room == null) {
            return Collections.emptyMap();
        }
        return Map.copyOf(room);
    }

    public SquadMemberLiveState updateStation(UUID squadId, UUID userId, String stationCode) {
        return upsert(squadId, userId, stationCode, null, null);
    }

    public SquadMemberLiveState updateProgress(UUID squadId, UUID userId, Integer percent, String label) {
        return upsert(squadId, userId, null, percent, label);
    }

    private SquadMemberLiveState upsert(
            UUID squadId, UUID userId, String stationCode, Integer percent, String label) {
        Map<UUID, SquadMemberLiveState> room =
                bySquad.computeIfAbsent(squadId, ignored -> new ConcurrentHashMap<>());
        SquadMemberLiveState prev = room.getOrDefault(userId, SquadMemberLiveState.empty(userId));
        String nextStation = stationCode != null ? stationCode : prev.stationCode();
        Integer nextPercent = percent != null ? percent : prev.progressPercent();
        String nextLabel = label != null ? label : prev.progressLabel();
        SquadMemberLiveState next =
                new SquadMemberLiveState(userId, nextStation, nextPercent, nextLabel, Instant.now());
        room.put(userId, next);
        return next;
    }

    public void clearUser(UUID squadId, UUID userId) {
        Map<UUID, SquadMemberLiveState> room = bySquad.get(squadId);
        if (room != null) {
            room.remove(userId);
        }
    }
}
