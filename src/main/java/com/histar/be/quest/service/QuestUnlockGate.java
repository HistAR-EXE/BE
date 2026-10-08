package com.histar.be.quest.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.quest.entity.Quest;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuestUnlockGate {

    private final UserDiscoveryRepository userDiscoveryRepository;

    public void assertUnlocked(UUID userId, Quest quest) {
        String raw = quest.getUnlockDiscoveryKeys();
        if (raw == null || raw.isBlank()) {
            return;
        }
        Set<String> have = userDiscoveryRepository
                .findByUserIdAndLocationId(userId, quest.getLocationId())
                .stream()
                .map(UserDiscovery::getDiscoveryKey)
                .collect(Collectors.toSet());
        for (String key : raw.split(",")) {
            if (!key.isBlank() && !have.contains(key.trim())) {
                throw new BusinessRuleException(
                        "Mật lệnh này mở khi đã xem hai cảnh trong tour của mật lệnh đầu.");
            }
        }
    }
}
