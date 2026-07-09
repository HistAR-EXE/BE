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

    @Column(name = "require_onsite_checkin", nullable = false)
    @Builder.Default
    private Boolean requireOnsiteCheckin = false;

    // ----- THÊM MỚI QUAN HỆ VỚI QUEST_STEPS -----
    @OneToMany(mappedBy = "quest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    @Builder.Default
    private List<QuestStep> steps = new ArrayList<>();
}