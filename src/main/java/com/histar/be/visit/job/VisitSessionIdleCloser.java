package com.histar.be.visit.job;

import com.histar.be.config.VisitSessionProperties;
import com.histar.be.visit.service.impl.VisitSessionServiceImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class VisitSessionIdleCloser {

    private final VisitSessionServiceImpl visitSessionService;
    private final VisitSessionProperties visitSessionProperties;

    @Scheduled(cron = "0 */5 * * * *")
    public void closeIdleSessions() {
        Instant cutoff = Instant.now().minus(visitSessionProperties.getIdleTimeoutMinutes(), ChronoUnit.MINUTES);
        int closed = visitSessionService.closeIdleSessions(cutoff);
        if (closed > 0) {
            log.info("Closed {} idle visit sessions (cutoff={})", closed, cutoff);
        }
    }
}
