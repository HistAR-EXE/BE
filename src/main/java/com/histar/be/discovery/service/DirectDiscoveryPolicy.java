package com.histar.be.discovery.service;

import com.histar.be.quest.repository.QuestStepRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Khóa nào client được ghi thẳng qua {@code POST /api/me/discoveries} (chỉ là "đã xem").
 * Thử thách (dialogue, minigame…) và sự kiện server (check-in) có luồng xác nhận riêng.
 */
@Component
@RequiredArgsConstructor
public class DirectDiscoveryPolicy {

    private static final List<String> VIEW_PREFIXES =
            List.of("scene:", "photo:", "hotspot:", "era:", "depth:", "tour:", "xuanay:");
    private static final String ARTIFACT_PREFIX = "artifact:";

    private final QuestStepRepository questStepRepository;

    public boolean allows(UUID locationId, String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        for (String prefix : VIEW_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        // Hiện vật chỉ ghi thẳng khi là bước "xem hiện vật" của một nhiệm vụ Online (ví dụ 9 di tích ngoài Củ Chi).
        // Hiện vật sưu tầm ở Củ Chi không phải bước như vậy nên vẫn chỉ mở bằng check-in / minigame.
        return key.startsWith(ARTIFACT_PREFIX)
                && locationId != null
                && questStepRepository.existsOnlineArtifactStep(locationId, key);
    }
}
