package com.histar.be.billing.repository;

import com.histar.be.billing.entity.UsageQuota;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageQuotaRepository extends JpaRepository<UsageQuota, UUID> {

    Optional<UsageQuota> findByUserIdAndYearAndMonthAndDay(UUID userId, int year, int month, int day);

    Optional<UsageQuota> findByOrganizationIdAndYearAndMonth(UUID organizationId, int year, int month);
}
