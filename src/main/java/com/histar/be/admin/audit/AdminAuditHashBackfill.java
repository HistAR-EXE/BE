package com.histar.be.admin.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
public class AdminAuditHashBackfill implements ApplicationRunner {

    private final AdminAuditService auditService;

    @Override
    public void run(ApplicationArguments args) {
        int filled = auditService.backfillHashChainIfNeeded();
        if (filled > 0) {
            log.info("Backfilled SHA-256 hash chain for {} admin audit events", filled);
        }
    }
}
