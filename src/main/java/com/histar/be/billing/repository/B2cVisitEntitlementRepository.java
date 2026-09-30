package com.histar.be.billing.repository;

import com.histar.be.billing.entity.B2cVisitEntitlement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface B2cVisitEntitlementRepository extends JpaRepository<B2cVisitEntitlement, UUID> {

    Optional<B2cVisitEntitlement> findFirstByUserIdAndSiteCodeAndExpiresAtAfterOrderByExpiresAtDesc(
            UUID userId, String siteCode, Instant now);

    boolean existsByUserIdAndSiteCodeAndExpiresAtAfter(UUID userId, String siteCode, Instant now);

    List<B2cVisitEntitlement> findByUserIdAndExpiresAtAfterOrderByExpiresAtDesc(UUID userId, Instant now);
}
