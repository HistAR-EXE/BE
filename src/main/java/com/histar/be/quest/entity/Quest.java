package com.histar.be.quest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    /** Reserved for future step ordering; evaluator does not enforce (BR-QST-06). */
    private Integer requiredOrder;
    private String completionTrigger;

    /** Comma-separated discovery unlock_keys required before check-in completes quest. */
    @Column(name = "step_discovery_keys", columnDefinition = "text")
    private String stepDiscoveryKeys;
}
