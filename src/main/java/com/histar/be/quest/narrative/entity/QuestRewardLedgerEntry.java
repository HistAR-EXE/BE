package com.histar.be.quest.narrative.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "quest_reward_ledger")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestRewardLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "quest_id", nullable = false) private UUID questId;
    @Column(name = "reward_type", nullable = false) private String rewardType;
    @Column(name = "reward_key", nullable = false) private String rewardKey;
    @Column(nullable = false) private int xp;
    @Column(name = "awarded_at", nullable = false) private Instant awardedAt;
}
