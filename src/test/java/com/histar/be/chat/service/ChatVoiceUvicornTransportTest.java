package com.histar.be.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import com.histar.be.config.ChatProperties;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ChatVoiceUvicornTransportTest {
    @Test void postsJsonToRealUvicornWithoutAnHttp2Upgrade() throws Exception {
        Path ai = Path.of("../AI").toAbsolutePath().normalize();
        Path python = ai.resolve(System.getProperty("os.name").startsWith("Windows")
                ? ".venv/Scripts/python.exe" : ".venv/bin/python");
        assumeTrue(Files.isRegularFile(python), "Local cross-service test requires AI/.venv");
        int port;
        try (var socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        var builder = new ProcessBuilder(python.toString(), "tests/voice_transport_fixture.py", Integer.toString(port))
                .directory(ai.toFile()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD);
        builder.environment().put("WARMUP_ON_START", "false");
        builder.environment().put("VOICE_WARMUP", "false");
        Process server = builder.start();
        try {
            var probe = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(1)).build();
            boolean ready = false;
            for (int i = 0; i < 60 && server.isAlive(); i++) {
                try {
                    ready = probe.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/ai/health"))
                            .timeout(Duration.ofSeconds(1)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
                } catch (Exception ignored) { /* startup */ }
                if (ready) break;
                Thread.sleep(200);
            }
            assertThat(ready).as("Uvicorn fixture started").isTrue();
            var config = new ChatProperties();
            config.getRagAi().setBaseUrl("http://127.0.0.1:" + port);
            var result = new ChatVoiceService(config).synthesize("Xin chào Chrono!", "chrono");
            assertThat(result.contentType()).isEqualTo("audio/mpeg");
            assertThat(new String(result.bytes(), StandardCharsets.UTF_8)).isEqualTo("ID3|Xin chào Chrono!");
        } finally {
            server.destroy();
            if (!server.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) server.destroyForcibly();
        }
    }
}
