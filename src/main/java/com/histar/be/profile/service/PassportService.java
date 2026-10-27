package com.histar.be.profile.service;

import com.histar.be.billing.service.UsageQuotaService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.dto.PassportResponse;
import com.histar.be.profile.dto.PassportStampResponse;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PassportService {

    private final CheckinRepository checkinRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final LocationRepository locationRepository;
    private final QuestRepository questRepository;
    private final UsageQuotaService usageQuotaService;

    @Transactional(readOnly = true)
    public PassportResponse getPassport(UUID userId) {
        boolean arStampEligible = usageQuotaService.hasPremiumEntitlement(userId);
        Map<UUID, Instant> stampTimes = new HashMap<>();

        for (Checkin checkin : checkinRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            if (checkin.getLocationId() == null) {
                continue;
            }
            recordStampTime(stampTimes, checkin.getLocationId(), checkin.getCreatedAt());
        }

        List<UserQuestProgress> completed =
                userQuestProgressRepository.findByUserIdAndStatus(userId, "completed");
        Set<UUID> questIds = new HashSet<>();
        for (UserQuestProgress progress : completed) {
            if (progress.getQuestId() != null) {
                questIds.add(progress.getQuestId());
            }
        }
        Map<UUID, Quest> questsById = new HashMap<>();
        if (!questIds.isEmpty()) {
            questRepository.findByIdInAndDeletedAtIsNull(questIds).forEach(q -> questsById.put(q.getId(), q));
        }
        for (UserQuestProgress progress : completed) {
            UUID locationId = progress.getLocationId();
            if (locationId == null && progress.getQuestId() != null) {
                Quest quest = questsById.get(progress.getQuestId());
                if (quest != null) {
                    locationId = quest.getLocationId();
                }
            }
            if (locationId == null) {
                continue;
            }
            // A task start is not evidence of its completion time. Keep an unknown date unknown.
            recordStampTime(stampTimes, locationId, progress.getCompletedAt());
        }

        Set<UUID> locationIds = stampTimes.keySet();
        Map<UUID, String> locationNames = new HashMap<>();
        if (!locationIds.isEmpty()) {
            locationRepository.findAllById(locationIds).forEach(loc -> locationNames.put(loc.getId(), loc.getName()));
        }

        List<PassportStampResponse> stamps = new ArrayList<>();
        for (Map.Entry<UUID, Instant> entry : stampTimes.entrySet()) {
            UUID locationId = entry.getKey();
            stamps.add(new PassportStampResponse(
                    locationId,
                    locationNames.getOrDefault(locationId, "Di sản"),
                    entry.getValue(),
                    arStampEligible));
        }
        stamps.sort(Comparator.comparing(PassportStampResponse::completedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new PassportResponse(stamps);
    }

    private Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private void recordStampTime(Map<UUID, Instant> stampTimes, UUID locationId, Instant at) {
        if (at == null) {
            stampTimes.putIfAbsent(locationId, null);
        } else {
            stampTimes.merge(locationId, at, this::latest);
        }
    }
}
