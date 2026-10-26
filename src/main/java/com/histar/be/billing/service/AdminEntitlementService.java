package com.histar.be.billing.service;

import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserTier;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies admin grants through the same expiring subscription model used by paid B2C access. */
@Service
@RequiredArgsConstructor
public class AdminEntitlementService {
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final B2cSubscriptionRepository subscriptions;

    @Transactional
    public void changeTier(Profile profile, UserTier tier) {
        if (profile.getOrgId() != null) {
            throw new BusinessRuleException("Tài khoản thuộc tổ chức: hãy thay đổi gói tại trang Tổ chức.");
        }
        subscriptions.findByUserIdAndIsActiveTrue(profile.getId()).ifPresent(active -> {
            active.setIsActive(false);
            subscriptions.saveAndFlush(active);
        });
        if (tier == UserTier.PREMIUM) {
            LocalDate today = LocalDate.now(VN_ZONE);
            subscriptions.save(B2cSubscription.builder()
                    .userId(profile.getId())
                    .priceVnd(0)
                    .startDate(today)
                    .endDate(today.plusMonths(1))
                    .isActive(true)
                    .paymentMethod("ADMIN_GRANT")
                    .createdAt(Instant.now())
                    .build());
        }
        profile.setTier(tier.name());
    }
}
