package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.*;
import com.histar.be.config.ChatProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;

class ChatVoiceServiceTest {
    HttpServer server;
    ChatVoiceService service;
    AtomicReference<String> request = new AtomicReference<>();
    AtomicReference<String> upgrade = new AtomicReference<>();
    int status = 200;
    String type = "audio/mpeg";
    byte[] audio = "ID3-test-audio".getBytes(StandardCharsets.UTF_8);

    @BeforeEach void setup() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ai/voice/tts", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            upgrade.set(exchange.getRequestHeaders().getFirst("Upgrade"));
            exchange.getResponseHeaders().set("Content-Type", type);
            exchange.sendResponseHeaders(status, audio.length);
            exchange.getResponseBody().write(audio);
            exchange.close();
        });
        server.start();
        var properties = new ChatProperties();
        properties.getRagAi().setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        service = new ChatVoiceService(properties);
    }
    @AfterEach void cleanup() { server.stop(0); }

    @Test void proxiesVietnameseAudioWithoutAClientVisibleAiUrl() {
        assertThat(service.synthesize("Xin chào bạn!", null).bytes()).isEqualTo(audio);
        assertThat(request.get()).contains("Xin chào bạn!", "chrono");
        assertThat(upgrade.get()).isNull();
    }
    @Test void rejectsNonAudioEvenWhenUpstreamReturns200() {
        type = "application/json";
        assertThatThrownBy(() -> service.synthesize("Xin chào", "chrono")).isInstanceOf(RagServiceException.class);
    }
    @Test void doesNotExposeUpstreamFailureBodies() {
        status = 503;
        audio = "private upstream detail".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> service.synthesize("Xin chào", "chrono"))
                .isInstanceOf(RagServiceException.class).hasMessageNotContaining("private upstream");
    }
}
