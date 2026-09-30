package com.histar.be.squad.config;

import com.histar.be.squad.websocket.SquadWebSocketHandler;
import com.histar.be.squad.websocket.SquadWebSocketHandshakeInterceptor;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class SquadWebSocketConfig implements WebSocketConfigurer {

    private final SquadWebSocketHandler squadWebSocketHandler;
    private final SquadWebSocketHandshakeInterceptor handshakeInterceptor;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        var registration = registry.addHandler(squadWebSocketHandler, "/ws/squad")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins(origins.length > 0 ? origins : new String[] {"*"});
        registration.withSockJS();
    }
}
