package com.histar.be.device.service;

import com.histar.be.config.HistarFirebaseProperties;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Sends FCM notifications when Firebase Admin is configured; otherwise logs only (E2 stub). */
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmPushSendService {

    private final HistarFirebaseProperties firebaseProperties;

    public void sendToUser(UUID userId, String title, String body, List<String> deviceTokens) {
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.debug("FCM skip userId={}: no device tokens", userId);
            return;
        }
        if (!firebaseProperties.isEnabled() || firebaseProperties.getServiceAccountJson().isBlank()) {
            log.info(
                    "FCM stub: would push to userId={} tokens={} title=\"{}\" body=\"{}\"",
                    userId,
                    deviceTokens.size(),
                    title,
                    body);
            return;
        }
        log.info(
                "FCM send not wired in E2 stub — userId={} tokens={} title=\"{}\" (enable full sender later)",
                userId,
                deviceTokens.size(),
                title);
    }
}
