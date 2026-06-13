package com.histar.be.visit.service.impl;

import com.histar.be.discovery.entity.DiscoveryPoint;
import com.histar.be.discovery.repository.DiscoveryPointRepository;
import com.histar.be.location.service.LocationService;
import com.histar.be.visit.entity.EndReason;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.entity.VisitSessionEvent;
import com.histar.be.visit.repository.VisitSessionEventRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import com.histar.be.visit.service.VisitSessionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitSessionServiceImpl implements VisitSessionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CLOSED = "CLOSED";

    private final VisitSessionRepository visitSessionRepository;
    private final VisitSessionEventRepository visitSessionEventRepository;
    private final DiscoveryPointRepository discoveryPointRepository;
    private final LocationService locationService;

    @Override
    @Transactional
    public UUID startSession(UUID userId, UUID locationId, String mode) {
        closeActiveSessions(userId, locationId, EndReason.SYSTEM);
        Instant now = Instant.now();
        VisitSession session = VisitSession.builder()
                .userId(userId)
                .locationId(locationId)
                .mode(mode != null ? mode : "online")
                .status(STATUS_ACTIVE)
                .startedAt(now)
                .lastActivityAt(now)
                .build();
        return visitSessionRepository.save(session).getId();
    }

    @Override
    @Transactional
    public void endSession(UUID userId, UUID sessionId) {
        endSession(userId, sessionId, EndReason.USER_EXIT);
    }

    @Override
    @Transactional
    public void endSession(UUID userId, UUID sessionId, EndReason reason) {
        visitSessionRepository.findById(sessionId).ifPresent(session -> {
            if (session.getUserId().equals(userId) && session.getEndedAt() == null) {
                Instant now = Instant.now();
                session.setEndedAt(now);
                session.setStatus(STATUS_CLOSED);
                session.setEndedReason(reason != null ? reason.name() : EndReason.USER_EXIT.name());
                if (session.getLastActivityAt() == null) {
                    session.setLastActivityAt(now);
                }
                visitSessionRepository.save(session);
            }
        });
    }

    @Override
    @Transactional
    public void recordDiscoveryEvent(UUID userId, String unlockKey, String source, UUID locationId) {
        if (locationId == null) {
            return;
        }
        VisitSession session = resolveActiveSession(userId, locationId, "online");
        touchActivity(session);
        if (visitSessionEventRepository.existsByVisitSessionIdAndEventTypeAndEventKey(
                session.getId(), "discovery", unlockKey)) {
            return;
        }
        String poiName = discoveryPointRepository
                .findByUnlockKeyAndLocationId(unlockKey, locationId)
                .map(DiscoveryPoint::getName)
                .orElse(unlockKey);
        visitSessionEventRepository.save(VisitSessionEvent.builder()
                .visitSessionId(session.getId())
                .eventType("discovery")
                .eventKey(unlockKey)
                .source(source)
                .poiNameSnapshot(poiName)
                .createdAt(Instant.now())
                .build());
    }

    @Override
    @Transactional
    public void recordCheckinEvent(UUID userId, UUID locationId, UUID checkinId) {
        VisitSession session = resolveActiveSession(userId, locationId, "offline");
        session.setCheckinId(checkinId);
        touchActivity(session);
        visitSessionRepository.save(session);
        String locationName = locationService.findById(locationId).getName();
        visitSessionEventRepository.save(VisitSessionEvent.builder()
                .visitSessionId(session.getId())
                .eventType("checkin")
                .eventKey(locationId.toString())
                .source("offline")
                .poiNameSnapshot(locationName)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional
    public int closeIdleSessions(Instant cutoff) {
        List<VisitSession> idle = visitSessionRepository.findIdleActiveSessions(STATUS_ACTIVE, cutoff);
        Instant now = Instant.now();
        for (VisitSession session : idle) {
            session.setEndedAt(now);
            session.setStatus(STATUS_CLOSED);
            session.setEndedReason(EndReason.TIMEOUT.name());
        }
        if (!idle.isEmpty()) {
            visitSessionRepository.saveAll(idle);
        }
        return idle.size();
    }

    private VisitSession resolveActiveSession(UUID userId, UUID locationId, String defaultMode) {
        return visitSessionRepository
                .findFirstByUserIdAndLocationIdAndStatusAndEndedAtIsNullOrderByStartedAtDesc(
                        userId, locationId, STATUS_ACTIVE)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    return visitSessionRepository.save(VisitSession.builder()
                            .userId(userId)
                            .locationId(locationId)
                            .mode(defaultMode)
                            .status(STATUS_ACTIVE)
                            .startedAt(now)
                            .lastActivityAt(now)
                            .build());
                });
    }

    private void touchActivity(VisitSession session) {
        session.setLastActivityAt(Instant.now());
        visitSessionRepository.save(session);
    }

    private void closeActiveSessions(UUID userId, UUID locationId, EndReason reason) {
        List<VisitSession> actives = visitSessionRepository
                .findByUserIdAndLocationIdAndStatusAndEndedAtIsNull(userId, locationId, STATUS_ACTIVE);
        Instant now = Instant.now();
        for (VisitSession session : actives) {
            session.setEndedAt(now);
            session.setStatus(STATUS_CLOSED);
            session.setEndedReason(reason.name());
            if (session.getLastActivityAt() == null) {
                session.setLastActivityAt(now);
            }
        }
        if (!actives.isEmpty()) {
            visitSessionRepository.saveAll(actives);
        }
    }
}
