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
public class B2cExpiryJob {

    private final BillingService billingService;

    @Scheduled(cron = "0 20 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "b2cExpiry", lockAtMostFor = "PT20M", lockAtLeastFor = "PT5M")
    public void reconcileExpiredB2c() {
        int affected = billingService.reconcileExpiredB2cSubscriptions();
        if (affected > 0) {
            log.info("B2cExpiryJob expired {} B2C subscription(s) and downgraded eligible profiles to FREE", affected);
        }
    }
}
