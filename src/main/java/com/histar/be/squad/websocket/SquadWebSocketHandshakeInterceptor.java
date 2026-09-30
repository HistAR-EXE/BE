package com.histar.be.squad.websocket;

import com.histar.be.security.JwtService;
import com.histar.be.squad.repository.SquadMemberRepository;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class SquadWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final SquadMemberRepository squadMemberRepository;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        String squadIdRaw = servletRequest.getServletRequest().getParameter("squadId");
        if (token == null || token.isBlank() || squadIdRaw == null || squadIdRaw.isBlank()) {
            return false;
        }
        if (!jwtService.isValid(token)) {
            return false;
        }
        Optional<UUID> userIdOpt = jwtService.extractUserId(token);
        if (userIdOpt.isEmpty()) {
            return false;
        }
        UUID squadId;
        try {
            squadId = UUID.fromString(squadIdRaw.trim());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        UUID userId = userIdOpt.get();
        if (!squadMemberRepository.existsBySquadIdAndUserId(squadId, userId)) {
            return false;
        }
        attributes.put(SquadWebSocketAttributes.USER_ID, userId);
        attributes.put(SquadWebSocketAttributes.SQUAD_ID, squadId);
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {}
}
