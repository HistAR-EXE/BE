package com.histar.be.squad.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.squad.entity.SquadMember;
import com.histar.be.squad.repository.SquadMemberRepository;
import com.histar.be.squad.sync.SquadLiveStateService;
import com.histar.be.squad.sync.SquadMemberLiveState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class SquadWebSocketBroadcaster {

    private final SquadWebSocketSessionRegistry registry;
    private final SquadLiveStateService liveStateService;
    private final SquadMemberRepository squadMemberRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public void sendSnapshot(UUID squadId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "snapshot");
        payload.put("squadId", squadId.toString());
        payload.put("members", buildMemberPayload(squadId));
        broadcast(squadId, payload);
    }

    public void sendMemberUpdate(UUID squadId, UUID userId, SquadMemberLiveState state) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "member_update");
        payload.put("squadId", squadId.toString());
        payload.put("userId", userId.toString());
        payload.put("stationCode", state.stationCode());
        payload.put("progressPercent", state.progressPercent());
        payload.put("progressLabel", state.progressLabel());
        payload.put("updatedAt", state.updatedAt() != null ? state.updatedAt().toString() : null);
        broadcast(squadId, payload);
    }

    private List<Map<String, Object>> buildMemberPayload(UUID squadId) {
        Map<UUID, SquadMemberLiveState> live = liveStateService.snapshot(squadId);
        List<SquadMember> roster = squadMemberRepository.findBySquadIdOrderByJoinedAtAsc(squadId);
        List<Map<String, Object>> members = new ArrayList<>();
        for (SquadMember member : roster) {
            UUID userId = member.getUserId();
            SquadMemberLiveState state = live.getOrDefault(userId, SquadMemberLiveState.empty(userId));
            Map<String, Object> row = new HashMap<>();
            row.put("userId", userId.toString());
            profileRepository.findById(userId).ifPresent(profile -> {
                row.put("displayName", profile.getDisplayName());
                row.put("avatarUrl", profile.getAvatarUrl());
            });
            row.put("joinedAt", member.getJoinedAt().toString());
            row.put("stationCode", state.stationCode());
            row.put("progressPercent", state.progressPercent());
            row.put("progressLabel", state.progressLabel());
            row.put("updatedAt", state.updatedAt() != null ? state.updatedAt().toString() : null);
            members.add(row);
        }
        return members;
    }

    private void broadcast(UUID squadId, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage message = new TextMessage(json);
            for (WebSocketSession session : registry.sessions(squadId)) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (Exception ignored) {
            // best-effort fan-out
        }
    }
}
