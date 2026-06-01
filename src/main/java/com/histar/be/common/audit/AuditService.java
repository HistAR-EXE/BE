package com.histar.be.common.audit;

import java.util.UUID;

public interface AuditService {

    void log(AuditAction action, String resourceType, UUID resourceId, UUID actorId, String detail);
}
