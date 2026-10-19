package com.histar.be.quest.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "quests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "row_version", nullable = false)
    private Long rowVersion;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @Column(name = "delete_reason", columnDefinition = "text")
    private String deleteReason;

    private UUID locationId;
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(columnDefinition = "text")
    private String story;

    private Integer pointsReward;

    private Integer requiredOrder;
    private String completionTrigger;
    private Integer stepsTotal;
    private String coverImage;

    @Column(name = "step_discovery_keys", columnDefinition = "text")
    private String stepDiscoveryKeys;

    @Column(name = "unlock_after_quest_id")
    private UUID unlockAfterQuestId;

    @Column(name = "unlock_discovery_keys", columnDefinition = "text")
    private String unlockDiscoveryKeys;

    @Column(name = "require_onsite_checkin", nullable = false)
    @Builder.Default
    private Boolean requireOnsiteCheckin = false;

    // ----- NHIỆM VỤ V2 -----
    /** ONLINE (làm từ xa) hoặc ONSITE (phải có mặt tại di tích). */
    @Column(name = "track", nullable = false, length = 16)
    @Builder.Default
    private String track = "ONLINE";

    /** Thứ tự hiển thị trong cùng di tích + nhóm. */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 100;

    /** EASY / MEDIUM / HARD. */
    @Column(name = "difficulty", length = 16)
    private String difficulty;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    // ----- THÊM MỚI QUAN HỆ VỚI QUEST_STEPS -----
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<QuestStep> steps = new ArrayList<>();
}
