package com.histar.be.device.repository;

import com.histar.be.device.entity.DevicePushToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, UUID> {

    Optional<DevicePushToken> findByUserIdAndPlatform(UUID userId, String platform);

    List<DevicePushToken> findByUserId(UUID userId);
}
