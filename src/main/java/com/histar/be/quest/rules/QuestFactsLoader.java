package com.histar.be.quest.rules;

import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Nạp dữ kiện của người dùng tại một di tích (một truy vấn cho mỗi loại dữ liệu). */
@Component
@RequiredArgsConstructor
public class QuestFactsLoader {

    private static final String QR = "QR";

    private final UserDiscoveryRepository userDiscoveryRepository;
    private final CheckinRepository checkinRepository;
    private final UsageQuotaService usageQuotaService;

    /** Dữ kiện đầy đủ, kèm quyền Premium (dùng để hiển thị bước bị khóa). */
    public QuestRules.Facts load(UUID userId, UUID locationId) {
        return load(userId, locationId, true);
    }

    public QuestRules.Facts load(UUID userId, UUID locationId, boolean includePremium) {
        if (userId == null || locationId == null) {
            return QuestRules.Facts.empty();
        }
        Map<String, Instant> discoveries = new HashMap<>();
        for (UserDiscovery discovery : userDiscoveryRepository.findByUserIdAndLocationId(userId, locationId)) {
            if (discovery.getDiscoveryKey() == null) {
                continue;
            }
            Instant at = discovery.getDiscoveredAt() != null ? discovery.getDiscoveredAt() : Instant.EPOCH;
            discoveries.merge(discovery.getDiscoveryKey(), at, QuestFactsLoader::earliest);
        }
        Map<String, Instant> stations = new HashMap<>();
        Instant firstCheckin = null;
        for (Checkin checkin : checkinRepository.findByUserIdAndLocationId(userId, locationId)) {
            Instant at = checkin.getCreatedAt() != null ? checkin.getCreatedAt() : Instant.EPOCH;
            firstCheckin = firstCheckin == null ? at : earliest(firstCheckin, at);
            String code = checkin.getStationCode();
            if (code != null && !code.isBlank() && QR.equalsIgnoreCase(checkin.getPresenceMethod())) {
                stations.merge(code.trim().toUpperCase(Locale.ROOT), at, QuestFactsLoader::earliest);
            }
        }
        boolean premium = includePremium && usageQuotaService.hasPremiumEntitlement(userId);
        return new QuestRules.Facts(Map.copyOf(discoveries), Map.copyOf(stations), firstCheckin, premium);
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
