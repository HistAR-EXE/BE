package com.histar.be.billing.service;

import com.histar.be.billing.entity.B2cRenewalReminder;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.repository.B2cRenewalReminderRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2cRenewalReminderService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final List<Integer> REMINDER_DAYS = List.of(7, 3, 1);

    private final B2cSubscriptionRepository b2cSubscriptionRepository;
    private final B2cRenewalReminderRepository b2cRenewalReminderRepository;
    private final ProfileRepository profileRepository;
    private final HistarEmailService histarEmailService;
    private final HistarMailProperties mailProperties;

    @Transactional
    public int sendDailyRenewalReminders() {
        LocalDate today = LocalDate.now(VN_ZONE);
        int sent = 0;
        for (B2cSubscription sub : b2cSubscriptionRepository.findAll()) {
            if (!Boolean.TRUE.equals(sub.getIsActive()) || sub.getEndDate() == null) {
                continue;
            }
            int daysUntil = (int) ChronoUnit.DAYS.between(today, sub.getEndDate());
            if (!REMINDER_DAYS.contains(daysUntil)) {
                continue;
            }
            if (b2cRenewalReminderRepository.existsBySubscriptionIdAndReminderDay(sub.getId(), daysUntil)) {
                continue;
            }
            Profile profile = profileRepository.findById(sub.getUserId()).orElse(null);
            if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
                continue;
            }
            sendReminderEmail(profile.getEmail(), daysUntil, sub.getEndDate());
            b2cRenewalReminderRepository.save(B2cRenewalReminder.builder()
                    .subscriptionId(sub.getId())
                    .reminderDay(daysUntil)
                    .sentAt(Instant.now())
                    .build());
            sent += 1;
        }
        return sent;
    }

    private void sendReminderEmail(String to, int daysUntil, LocalDate endDate) {
        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — B2C renewal reminder {} day(s) for {} endDate={}", daysUntil, to, endDate);
            return;
        }
        String text = "Gói Premium của bạn sẽ hết hạn sau " + daysUntil
                + " ngày (ngày hết hạn: " + endDate + "). Vui lòng gia hạn tại /checkout/b2c";
        histarEmailService.sendText(to, "[TimeLens] Nhắc gia hạn Premium", text);
    }
}
