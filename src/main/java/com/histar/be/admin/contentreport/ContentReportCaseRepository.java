package com.histar.be.admin.contentreport;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

public interface ContentReportCaseRepository
        extends JpaRepository<ContentReportCase, UUID>, JpaSpecificationExecutor<ContentReportCase> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ContentReportCase c join fetch c.event where c.eventId = :eventId")
    Optional<ContentReportCase> findByIdForUpdate(@Param("eventId") UUID eventId);

    @Override
    @EntityGraph(attributePaths = "event")
    Page<ContentReportCase> findAll(Specification<ContentReportCase> specification, Pageable pageable);

    long countByStatusIn(List<ContentReportStatus> statuses);
}
