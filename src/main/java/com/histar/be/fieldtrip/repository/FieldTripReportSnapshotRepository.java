package com.histar.be.fieldtrip.repository;

import com.histar.be.fieldtrip.entity.FieldTripReportSnapshot;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FieldTripReportSnapshotRepository extends JpaRepository<FieldTripReportSnapshot, UUID> {

    Optional<FieldTripReportSnapshot> findByFieldTripId(UUID fieldTripId);

    boolean existsByFieldTripId(UUID fieldTripId);
}
