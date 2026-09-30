package com.histar.be.liveboard.service;

import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.events.entity.PilotEvent;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.events.service.EventsService;
import com.histar.be.liveboard.dto.LiveBoardRow;
import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import com.histar.be.liveboard.dto.RallyResponse;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Teacher live board: aggregates org members' check-ins (+ optional pilot events) for the last 24h.
 *
 * <p>SSE design (Render 512MB friendly): one shared scheduled poll per org that has at least one
 * open stream; the snapshot is computed once per tick and fanned out to all emitters of that org.
 * Subscriber count is capped per org and globally.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiveBoardService {

    public static final String EVENT_LIVE_BOARD_RALLY = "live_board_rally";
    public static final Duration WINDOW = Duration.ofHours(24);
    static final long POLL_INTERVAL_MS = 5_000L;
    static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(30).toMillis();
    static final int MAX_STREAMS_PER_ORG = 20;
    static final int MAX_STREAMS_TOTAL = 60;

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ProfileRepository profileRepository;
    private final CheckinRepository checkinRepository;
    private final PilotEventRepository pilotEventRepository;

    private final Map<UUID, OrgChannel> channels = new ConcurrentHashMap<>();

    private static final class OrgChannel {
        final Set<SseEmitter> emitters = ConcurrentHashMap.newKeySet();
        volatile LiveBoardSnapshot last;
    }

    // ---------------------------------------------------------------- snapshot

    @Transactional(readOnly = true)
    public LiveBoardSnapshot snapshot(UUID orgId) {
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new BusinessRuleException("Organization không tồn tại"));
        Instant now = Instant.now();
        Instant since = now.minus(WINDOW);

        List<OrganizationMember> members = organizationMemberRepository.findByOrganizationId(orgId).stream()
                .filter(m -> !"teacher".equalsIgnoreCase(m.getOrgRole()))
                .toList();
        List<UUID> userIds = members.stream().map(OrganizationMember::getUserId).toList();
        Instant lastRally = lastRallyAt(orgId);
        if (userIds.isEmpty()) {
            return new LiveBoardSnapshot(orgId, now, since, 0, 0, lastRally, List.of());
        }

        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(userIds).forEach(p -> profiles.put(p.getId(), p));

        Map<UUID, Set<String>> stations = new HashMap<>();
        Map<UUID, Integer> scores = new HashMap<>();
        Map<UUID, Instant> lastAt = new HashMap<>();
        Map<UUID, String> lastStation = new HashMap<>();

        for (Checkin c : checkinRepository.findByUserIdInAndCreatedAtGreaterThanEqual(userIds, since)) {
            UUID uid = c.getUserId();
            if (c.getStationCode() != null && !c.getStationCode().isBlank()) {
                stations.computeIfAbsent(uid, k -> new HashSet<>()).add(c.getStationCode());
            }
            scores.merge(uid, c.getPresenceScore() != null ? c.getPresenceScore() : 0, Integer::sum);
            touch(lastAt, lastStation, uid, c.getCreatedAt(), c.getStationCode());
        }

        // Optional: pilot events (offline-synced station completions that may lack a checkin row).
        for (PilotEvent e : pilotEventRepository.findByUserIdInAndEventTypeAndOccurredAtGreaterThanEqual(
                userIds, EventsService.EVENT_STATION_COMPLETED, since)) {
            if (e.getStationCode() != null && !e.getStationCode().isBlank()) {
                stations.computeIfAbsent(e.getUserId(), k -> new HashSet<>()).add(e.getStationCode());
                touch(lastAt, lastStation, e.getUserId(), e.getOccurredAt(), e.getStationCode());
            }
        }

        List<LiveBoardRow> rows = new ArrayList<>(members.size());
        for (OrganizationMember m : members) {
            UUID uid = m.getUserId();
            Profile p = profiles.get(uid);
            rows.add(new LiveBoardRow(
                    uid,
                    p != null && p.getDisplayName() != null ? p.getDisplayName() : "Unknown",
                    p != null && p.getEmail() != null ? p.getEmail() : "",
                    stations.getOrDefault(uid, Set.of()).size(),
                    scores.getOrDefault(uid, 0),
                    lastStation.get(uid),
                    lastAt.get(uid)));
        }
        rows.sort(Comparator.comparingInt(LiveBoardRow::stationsCompleted)
                .reversed()
                .thenComparing(Comparator.comparingInt(LiveBoardRow::score).reversed())
                .thenComparing(LiveBoardRow::displayName, String.CASE_INSENSITIVE_ORDER));

        int active = (int) rows.stream().filter(r -> r.lastActivityAt() != null).count();
        return new LiveBoardSnapshot(orgId, now, since, rows.size(), active, lastRally, List.copyOf(rows));
    }

    private static void touch(
            Map<UUID, Instant> lastAt, Map<UUID, String> lastStation, UUID uid, Instant at, String station) {
        if (at == null) return;
        Instant prev = lastAt.get(uid);
        if (prev == null || at.isAfter(prev)) {
            lastAt.put(uid, at);
            if (station != null && !station.isBlank()) lastStation.put(uid, station);
        }
    }

    // --------------------------------------------------------------------- CSV

    public String exportCsv(UUID orgId) {
        LiveBoardSnapshot snap = snapshot(orgId);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("member,email,stations_completed,score,last_station,last_activity_at\r\n");
        for (LiveBoardRow r : snap.rows()) {
            sb.append(csv(r.displayName())).append(',')
                    .append(csv(r.email())).append(',')
                    .append(r.stationsCompleted()).append(',')
                    .append(r.score()).append(',')
                    .append(csv(r.lastStationCode())).append(',')
                    .append(r.lastActivityAt() != null ? r.lastActivityAt().toString() : "")
                    .append("\r\n");
        }
        return sb.toString();
    }

    static String csv(String value) {
        if (value == null) return "";
        String v = value;
        // Neutralise spreadsheet formula injection.
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }

    // ------------------------------------------------------------------- rally

    @Transactional
    public RallyResponse rally(UUID orgId, UUID teacherUserId) {
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new BusinessRuleException("Organization không tồn tại"));
        Instant now = Instant.now();
        pilotEventRepository.save(PilotEvent.builder()
                .userId(teacherUserId)
                .eventType(EVENT_LIVE_BOARD_RALLY)
                .payloadJson(rallyPayload(orgId))
                .occurredAt(now)
                .createdAt(now)
                .build());
        RallyResponse response = new RallyResponse(orgId, now);
        OrgChannel channel = channels.get(orgId);
        if (channel != null) {
            channel.emitters.forEach(e -> send(orgId, channel, e, "gather", response));
        }
        return response;
    }

    private Instant lastRallyAt(UUID orgId) {
        return pilotEventRepository
                .findFirstByEventTypeAndPayloadJsonContainingOrderByOccurredAtDesc(
                        EVENT_LIVE_BOARD_RALLY, rallyPayload(orgId))
                .map(PilotEvent::getOccurredAt)
                .orElse(null);
    }

    private static String rallyPayload(UUID orgId) {
        return "{\"orgId\":\"" + orgId + "\"}";
    }

    // --------------------------------------------------------------------- SSE

    public SseEmitter subscribe(UUID orgId) {
        int total = channels.values().stream().mapToInt(c -> c.emitters.size()).sum();
        OrgChannel existing = channels.get(orgId);
        if (total >= MAX_STREAMS_TOTAL || (existing != null && existing.emitters.size() >= MAX_STREAMS_PER_ORG)) {
            throw new BusinessRuleException("Live board đang đạt giới hạn kết nối, dùng polling thay thế");
        }
        LiveBoardSnapshot initial = snapshot(orgId);

        OrgChannel channel = channels.computeIfAbsent(orgId, k -> new OrgChannel());
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        Runnable cleanup = () -> remove(orgId, channel, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> cleanup.run());
        channel.emitters.add(emitter);
        if (channel.last == null) channel.last = initial;
        send(orgId, channel, emitter, "snapshot", initial);
        return emitter;
    }

    private void remove(UUID orgId, OrgChannel channel, SseEmitter emitter) {
        channel.emitters.remove(emitter);
        if (channel.emitters.isEmpty()) {
            channels.remove(orgId, channel);
        }
    }

    private void send(UUID orgId, OrgChannel channel, SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException ex) {
            remove(orgId, channel, emitter);
            try {
                emitter.complete();
            } catch (RuntimeException ignored) {
                // already completed
            }
        }
    }

    /** One DB poll per org with open streams; emits only when rows or rally changed, else heartbeat. */
    @Scheduled(fixedDelay = POLL_INTERVAL_MS, initialDelay = POLL_INTERVAL_MS)
    public void pollAndBroadcast() {
        if (channels.isEmpty()) return;
        for (Map.Entry<UUID, OrgChannel> entry : channels.entrySet()) {
            UUID orgId = entry.getKey();
            OrgChannel channel = entry.getValue();
            if (channel.emitters.isEmpty()) continue;
            try {
                LiveBoardSnapshot next = snapshot(orgId);
                LiveBoardSnapshot prev = channel.last;
                boolean changed = prev == null
                        || !prev.rows().equals(next.rows())
                        || !Objects.equals(prev.lastRallyAt(), next.lastRallyAt());
                channel.last = next;
                if (changed) {
                    channel.emitters.forEach(e -> send(orgId, channel, e, "snapshot", next));
                } else {
                    channel.emitters.forEach(e -> heartbeat(orgId, channel, e));
                }
            } catch (RuntimeException ex) {
                log.warn("Live board poll failed for org {}: {}", orgId, ex.getMessage());
            }
        }
    }

    private void heartbeat(UUID orgId, OrgChannel channel, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().comment("hb"));
        } catch (IOException | IllegalStateException ex) {
            remove(orgId, channel, emitter);
        }
    }
}
