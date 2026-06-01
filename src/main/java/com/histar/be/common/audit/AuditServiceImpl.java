package com.histar.be.common.audit;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditServiceImpl implements AuditService {

    @Override
    public void log(AuditAction action, String resourceType, UUID resourceId, UUID actorId, String detail) {
        log.debug(
                "audit action={} resourceType={} resourceId={} actorId={} detail={}",
                action,
                resourceType,
                resourceId,
                actorId,
                detail);
    }
}
