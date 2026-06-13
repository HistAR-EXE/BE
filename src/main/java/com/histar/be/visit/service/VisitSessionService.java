package com.histar.be.visit.service;

import com.histar.be.visit.entity.EndReason;
import java.util.UUID;

public interface VisitSessionService {

    UUID startSession(UUID userId, UUID locationId, String mode);

    void endSession(UUID userId, UUID sessionId);

    void endSession(UUID userId, UUID sessionId, EndReason reason);

    void recordDiscoveryEvent(UUID userId, String unlockKey, String source, UUID locationId);

    void recordCheckinEvent(UUID userId, UUID locationId, UUID checkinId);
}
