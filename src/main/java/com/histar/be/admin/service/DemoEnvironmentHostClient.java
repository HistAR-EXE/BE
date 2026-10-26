package com.histar.be.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.dto.DemoEnvironmentStatus;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DemoEnvironmentHostClient {
    private final DemoProperties properties;
    private final ObjectMapper objectMapper;

    public DemoEnvironmentStatus status() {
        return call("GET", "/status");
    }

    public DemoEnvironmentStatus start() {
        return call("POST", "/start");
    }

    public DemoEnvironmentStatus stop() {
        return call("POST", "/stop");
    }

    private DemoEnvironmentStatus call(String method, String path) {
        requireConfiguration();
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getControlUrl().replaceAll("/+$", "") + path))
                    .timeout(Duration.ofMinutes(6))
                    .header("Authorization", "Bearer " + properties.getControlSecret())
                    .header("Accept", "application/json");
            HttpRequest request = "POST".equals(method)
                    ? builder.POST(HttpRequest.BodyPublishers.noBody()).build()
                    : builder.GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 409) {
                int activeUsers = objectMapper.readTree(response.body()).path("activeUsers").asInt();
                throw new BusinessRuleException(
                        "Không thể tắt demo vì còn " + activeUsers + " người đang hoạt động");
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessRuleException("Dịch vụ điều khiển demo trả về lỗi " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), DemoEnvironmentStatus.class);
        } catch (BusinessRuleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessRuleException("Không thể kết nối dịch vụ điều khiển demo trên VPS");
        }
    }

    private void requireConfiguration() {
        if (properties.getControlUrl() == null || properties.getControlUrl().isBlank()
                || properties.getControlSecret() == null || properties.getControlSecret().length() < 32) {
            throw new BusinessRuleException("Dịch vụ điều khiển demo chưa được cấu hình");
        }
    }
}
