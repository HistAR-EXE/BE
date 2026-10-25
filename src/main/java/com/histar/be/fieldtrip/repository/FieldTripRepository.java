package com.histar.be.fieldtrip.repository;

import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldTripRepository extends JpaRepository<FieldTrip, UUID> {

    Optional<FieldTrip> findByJoinCodeIgnoreCase(String joinCode);

    boolean existsByJoinCodeIgnoreCase(String joinCode);

    List<FieldTrip> findByCreatedByOrderByCreatedAtDesc(UUID createdBy);

    List<FieldTrip> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<FieldTrip> findBySiteCodeIgnoreCaseOrderByVisitStartAsc(String siteCode);

    List<FieldTrip> findBySiteCodeIgnoreCaseAndStatusOrderByVisitStartAsc(
            String siteCode, FieldTripStatus status);
}
