package com.histar.be.gamification.rules.entity;

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
@Table(name = "unlock_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnlockRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID locationId;
    private String triggerType;
    private String triggerKey;
    private String rewardType;
    private String rewardKey;
    private boolean enabled;
    private Integer sortOrder;
    private Instant createdAt;
}
