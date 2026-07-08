package com.histar.be.billing.repository;

import com.histar.be.billing.entity.B2cSubscription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2cSubscriptionRepository extends JpaRepository<B2cSubscription, UUID> {

    Optional<B2cSubscription> findByUserIdAndIsActiveTrue(UUID userId);

    List<B2cSubscription> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
