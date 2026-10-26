package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.config.DemoProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DemoEnvironmentHostClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void readsHostStatusUsingThePrivateBearerSecret() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/status", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"enabled\":true,\"activeUsers\":2}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        Object client = newClient();
        Object status = client.getClass().getMethod("status").invoke(client);

        assertThat(status.getClass().getMethod("enabled").invoke(status)).isEqualTo(true);
        assertThat(status.getClass().getMethod("activeUsers").invoke(status)).isEqualTo(2);
        assertThat(authorization.get()).isEqualTo("Bearer " + "h".repeat(64));
    }

    @Test
    void surfacesTheActiveUserCountWhenTheHostRefusesShutdown() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stop", exchange -> {
            byte[] body = "{\"error\":\"demo_in_use\",\"activeUsers\":3}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(409, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        Object client = newClient();

        assertThatThrownBy(() -> invoke(client, "stop"))
                .hasMessageContaining("3 người đang hoạt động");
    }

    private Object newClient() throws Exception {
        DemoProperties properties = new DemoProperties();
        properties.setControlSecret("h".repeat(64));
        properties.setControlUrl("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            Class<?> type = Class.forName("com.histar.be.admin.service.DemoEnvironmentHostClient");
            return type.getConstructor(DemoProperties.class, ObjectMapper.class)
                    .newInstance(properties, new ObjectMapper());
        } catch (ClassNotFoundException exception) {
            fail("DemoEnvironmentHostClient is required to control the VPS demo stack");
            return null;
        }
    }

    private Object invoke(Object target, String method) throws Throwable {
        try {
            return target.getClass().getMethod(method).invoke(target);
        } catch (java.lang.reflect.InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
