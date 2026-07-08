package com.histar.be.lms.repository;

import com.histar.be.lms.entity.Assignment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {
    List<Assignment> findByOrgIdOrderByCreatedAtDesc(UUID orgId);

    List<Assignment> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
}
