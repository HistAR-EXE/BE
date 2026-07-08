package com.histar.be.billing.repository;

import com.histar.be.billing.entity.OrgBillingSubscription;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgBillingSubscriptionRepository extends JpaRepository<OrgBillingSubscription, UUID> {

    List<OrgBillingSubscription> findAllByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
