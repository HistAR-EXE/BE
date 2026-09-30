package com.histar.be.device.service;

import com.histar.be.device.dto.RegisterPushTokenRequest;
import com.histar.be.device.entity.DevicePushToken;
import com.histar.be.device.repository.DevicePushTokenRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DevicePushTokenService {

    private final DevicePushTokenRepository repository;

    @Transactional
    public void upsert(UUID userId, RegisterPushTokenRequest request) {
        String platform = request.platform().trim().toUpperCase();
        DevicePushToken row = repository
                .findByUserIdAndPlatform(userId, platform)
                .orElseGet(() -> DevicePushToken.builder()
                        .userId(userId)
                        .platform(platform)
                        .build());
        row.setToken(request.token().trim());
        row.setUpdatedAt(Instant.now());
        repository.save(row);
    }
}
