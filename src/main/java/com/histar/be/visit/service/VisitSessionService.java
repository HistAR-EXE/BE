package com.histar.be.visit.service;

import com.histar.be.visit.dto.StartVisitSessionRequest;
import com.histar.be.visit.entity.EndReason;
import java.util.UUID;

public interface VisitSessionService {

    UUID startSessionWithPersonalization(UUID userId, StartVisitSessionRequest request);

    default UUID startSession(UUID userId, UUID locationId, String mode) {
        return startSessionWithPersonalization(userId, new StartVisitSessionRequest(locationId, mode, "study", "30", "heritage"));
    }

    void endSession(UUID userId, UUID sessionId);

    void endSession(UUID userId, UUID sessionId, EndReason reason);

    void recordDiscoveryEvent(UUID userId, String unlockKey, String source, UUID locationId);

    void recordCheckinEvent(UUID userId, UUID locationId, UUID checkinId);
}