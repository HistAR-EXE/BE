package com.histar.be.quest.minigame;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_game_results",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "game_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserGameResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "best_stars", nullable = false)
    private int bestStars;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_level_id")
    private String lastLevelId;
}
