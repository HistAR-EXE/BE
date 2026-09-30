package com.histar.be.squad.websocket;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Component
public class SquadWebSocketSessionRegistry {

    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void register(UUID squadId, WebSocketSession session) {
        rooms.computeIfAbsent(squadId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(UUID squadId, WebSocketSession session) {
        Set<WebSocketSession> sessions = rooms.get(squadId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                rooms.remove(squadId, sessions);
            }
        }
    }

    public Set<WebSocketSession> sessions(UUID squadId) {
        Set<WebSocketSession> sessions = rooms.get(squadId);
        if (sessions == null) {
            return Collections.emptySet();
        }
        return Set.copyOf(sessions);
    }
}
