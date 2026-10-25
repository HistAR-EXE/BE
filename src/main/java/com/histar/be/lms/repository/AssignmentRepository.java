package com.histar.be.lms.repository;

import com.histar.be.lms.entity.Assignment;
import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    Optional<Assignment> findByFieldTripId(UUID fieldTripId);

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);

    long countByQuestIdAndDueAtIsNull(UUID questId);

    long countByQuestIdAndDueAtGreaterThanEqual(UUID questId, Instant now);
}
