package com.histar.be.lms.repository;

import com.histar.be.lms.entity.TeacherAiDraft;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherAiDraftRepository extends JpaRepository<TeacherAiDraft, UUID> {
    List<TeacherAiDraft> findByTeacherIdOrderByCreatedAtDesc(UUID teacherId);
}
