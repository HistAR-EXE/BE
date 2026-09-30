package com.histar.be.events.service;

import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.events.dto.EventBatchRequest;
import com.histar.be.events.dto.EventBatchResponse;
import com.histar.be.events.dto.PilotKpiResponse;
import com.histar.be.events.entity.PilotEvent;
import com.histar.be.events.repository.PilotEventRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventsService {

    public static final String EVENT_SESSION_START = "session_start";
    public static final String EVENT_SESSION_END = "session_end";
    public static final String EVENT_STATION_ARRIVED = "station_arrived";
    public static final String EVENT_STATION_COMPLETED = "station_completed";
    public static final String EVENT_SHARE_INITIATED = "share_initiated";
    public static final String EVENT_EXPORT_CREATED = "export_created";
    public static final String EVENT_PACK_LOADED = "pack_loaded";
    public static final String EVENT_CHECKIN_RESULT = "checkin_result";
    public static final String EVENT_NPS_SUBMITTED = "nps_submitted";
    public static final String EVENT_LANDING_VISIT = "landing_visit";
    public static final String EVENT_PAYWALL_SHOWN = "paywall_shown";
    public static final String EVENT_PURCHASE_SUCCESS = "purchase_success";
    public static final String EVENT_PORTAL_USED = "portal_used";
    public static final String EVENT_VIDEO_PLAYED = "video_played";
    public static final String EVENT_AUDIO_PLAYED = "audio_played";
    public static final String EVENT_GAME_COMPLETED = "game_completed";
    public static final String EVENT_CAMERA_OPENED = "camera_opened";
    public static final String EVENT_CHAT_MESSAGE = "chat_message";
    public static final String EVENT_CONTENT_REPORT = "content_report";
    public static final String EVENT_CONSENT_UPDATED = "consent_updated";

    /** Pilot event types accepted by {@code POST /api/events/batch}. */
    public static final List<String> PILOT_EVENT_TYPES = List.of(
            EVENT_SESSION_START,
            EVENT_SESSION_END,
            EVENT_STATION_ARRIVED,
            EVENT_STATION_COMPLETED,
            EVENT_SHARE_INITIATED,
            EVENT_EXPORT_CREATED,
            EVENT_PACK_LOADED,
            EVENT_CHECKIN_RESULT,
            EVENT_NPS_SUBMITTED,
            EVENT_LANDING_VISIT,
            EVENT_PAYWALL_SHOWN,
            EVENT_PURCHASE_SUCCESS,
            EVENT_PORTAL_USED,
            EVENT_VIDEO_PLAYED,
            EVENT_AUDIO_PLAYED,
            EVENT_GAME_COMPLETED,
            EVENT_CAMERA_OPENED,
            EVENT_CHAT_MESSAGE,
            EVENT_CONTENT_REPORT,
            EVENT_CONSENT_UPDATED);

    private static final Set<String> SUPPORTED_TYPES = Set.copyOf(PILOT_EVENT_TYPES);

    private final PilotEventRepository pilotEventRepository;
    private final CheckinRepository checkinRepository;

    public static boolean isSupportedEventType(String eventType) {
        return eventType != null && SUPPORTED_TYPES.contains(eventType.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public EventBatchResponse ingestBatch(EventBatchRequest request, UUID userId) {
        int accepted = 0;
        int skippedDuplicate = 0;
        int rejected = 0;
        Instant now = Instant.now();

        for (EventBatchRequest.PilotEventItem item : request.events()) {
            if (!isSupportedEventType(item.eventType())) {
                rejected++;
                continue;
            }
            if (item.clientUuid() != null && pilotEventRepository.existsByClientUuid(item.clientUuid())) {
                skippedDuplicate++;
                continue;
            }

            PilotEvent event = PilotEvent.builder()
                    .clientUuid(item.clientUuid())
                    .userId(userId)
                    .sessionId(item.sessionId())
                    .eventType(item.eventType().trim().toLowerCase(Locale.ROOT))
                    .stationCode(item.stationCode())
                    .payloadJson(item.payload())
                    .occurredAt(item.occurredAt())
                    .createdAt(now)
                    .build();
            pilotEventRepository.save(event);
            accepted++;
        }

        return new EventBatchResponse(accepted, skippedDuplicate, rejected);
    }

    @Transactional(readOnly = true)
    public PilotKpiResponse pilotKpiLast7Days() {
        Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
        Map<String, Long> byType = new LinkedHashMap<>();
        for (String type : PILOT_EVENT_TYPES) {
            byType.put(type, pilotEventRepository.countByEventTypeAndOccurredAtGreaterThanEqual(type, since));
        }
        long checkins = checkinRepository.countByCreatedAtGreaterThanEqual(since);
        return new PilotKpiResponse(
                byType.get(EVENT_SESSION_START),
                byType.get(EVENT_STATION_ARRIVED),
                byType.get(EVENT_SHARE_INITIATED),
                checkins,
                byType.get(EVENT_SESSION_END),
                byType.get(EVENT_STATION_COMPLETED),
                byType.get(EVENT_EXPORT_CREATED),
                byType.get(EVENT_PACK_LOADED),
                byType.get(EVENT_CHECKIN_RESULT),
                byType.get(EVENT_NPS_SUBMITTED),
                byType.get(EVENT_LANDING_VISIT),
                byType.get(EVENT_PAYWALL_SHOWN),
                byType.get(EVENT_PURCHASE_SUCCESS),
                byType);
    }
}