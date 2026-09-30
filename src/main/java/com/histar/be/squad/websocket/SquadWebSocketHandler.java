package com.histar.be.squad.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.squad.sync.SquadLiveStateService;
import com.histar.be.squad.sync.SquadMemberLiveState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class SquadWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final SquadWebSocketSessionRegistry registry;
    private final SquadWebSocketBroadcaster broadcaster;
    private final SquadLiveStateService liveStateService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        UUID squadId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.SQUAD_ID);
        UUID userId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.USER_ID);
        if (squadId == null || userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        registry.register(squadId, session);
        broadcaster.sendSnapshot(squadId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID squadId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.SQUAD_ID);
        UUID userId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.USER_ID);
        if (squadId == null || userId == null) {
            return;
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (Exception ex) {
            sendError(session, "Invalid JSON");
            return;
        }
        String type = root.path("type").asText("");
        switch (type) {
            case "join" -> broadcaster.sendSnapshot(squadId);
            case "station" -> {
                String stationCode = root.path("stationCode").asText(null);
                if (stationCode == null || stationCode.isBlank()) {
                    sendError(session, "stationCode required");
                    return;
                }
                SquadMemberLiveState state = liveStateService.updateStation(squadId, userId, stationCode.trim());
                broadcaster.sendMemberUpdate(squadId, userId, state);
            }
            case "progress" -> {
                JsonNode percentNode = root.get("percent");
                Integer percent = percentNode != null && !percentNode.isNull() ? percentNode.asInt() : null;
                String label = root.path("label").asText(null);
                SquadMemberLiveState state = liveStateService.updateProgress(squadId, userId, percent, label);
                broadcaster.sendMemberUpdate(squadId, userId, state);
            }
            default -> sendError(session, "Unknown type: " + type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID squadId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.SQUAD_ID);
        UUID userId = (UUID) session.getAttributes().get(SquadWebSocketAttributes.USER_ID);
        if (squadId != null) {
            registry.unregister(squadId, session);
        }
        if (squadId != null && userId != null) {
            liveStateService.clearUser(squadId, userId);
            broadcaster.sendSnapshot(squadId);
        }
    }

    private void sendError(WebSocketSession session, String msg) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "error");
            payload.put("message", msg);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception ignored) {
            // ignore
        }
    }
}
