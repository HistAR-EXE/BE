package com.histar.be.billing.job;

import com.histar.be.billing.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrgTrialExpiryJob {

    private final BillingService billingService;

    @Scheduled(cron = "0 15 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "orgTrialExpiry", lockAtMostFor = "PT20M", lockAtLeastFor = "PT5M")
    public void reconcileExpiredTrials() {
        int affected = billingService.reconcileExpiredTrials();
        if (affected > 0) {
            log.info("OrgTrialExpiryJob marked {} trial organizations as TRIAL_EXPIRED", affected);
        }
    }
}
