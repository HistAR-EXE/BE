package com.histar.be.minigame.entity;

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
@Table(name = "station_minigame_scores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationMinigameScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "minigame_id", nullable = false)
    private UUID minigameId;

    @Column(nullable = false)
    private int score;

    @Column(name = "completed_at", nullable = false)
    @Builder.Default
    private Instant completedAt = Instant.now();
}
