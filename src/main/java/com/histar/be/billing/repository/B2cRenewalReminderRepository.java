package com.histar.be.billing.repository;

import com.histar.be.billing.entity.B2cRenewalReminder;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2cRenewalReminderRepository extends JpaRepository<B2cRenewalReminder, UUID> {

    boolean existsBySubscriptionIdAndReminderDay(UUID subscriptionId, Integer reminderDay);
}
