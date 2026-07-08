package com.histar.be.billing.job;

import com.histar.be.billing.service.B2cRenewalReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class B2cRenewalReminderJob {

    private final B2cRenewalReminderService b2cRenewalReminderService;

    @Scheduled(cron = "0 30 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @SchedulerLock(name = "b2cRenewalReminder", lockAtMostFor = "PT20M", lockAtLeastFor = "PT5M")
    public void sendRenewalReminders() {
        int sent = b2cRenewalReminderService.sendDailyRenewalReminders();
        if (sent > 0) {
            log.info("B2cRenewalReminderJob sent {} reminder email(s)", sent);
        }
    }
}
