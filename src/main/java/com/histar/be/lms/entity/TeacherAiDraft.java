package com.histar.be.lms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "teacher_ai_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAiDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "org_id", nullable = false)
    private UUID orgId;
    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;
    @Column(name = "quest_id", nullable = false)
    private UUID questId;
    @Column(name = "campaign_id")
    private UUID campaignId;
    @Column(nullable = false)
    private String title;
    @Column(name = "source_version", nullable = false)
    private String sourceVersion;
    @Column(name = "answer_blocks_json", nullable = false, columnDefinition = "text")
    private String answerBlocksJson;
    @Column(name = "approved_locales_json", nullable = false, columnDefinition = "text")
    private String approvedLocalesJson;
    @Column(name = "evaluation_passed", nullable = false)
    private boolean evaluationPassed;
    @Column(name = "evaluation_reasons", nullable = false, columnDefinition = "text")
    private String evaluationReasons;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeacherAiDraftStatus status;
    @Column(name = "due_at")
    private Instant dueAt;
    @Column(name = "published_assignment_id")
    private UUID publishedAssignmentId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
