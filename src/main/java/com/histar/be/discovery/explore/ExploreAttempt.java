package com.histar.be.discovery.explore;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "explore_attempts", indexes = @Index(columnList = "user_id, submitted_at"))
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExploreAttempt {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "location_id", nullable = false) private UUID locationId;
    @Column(name = "challenge_key", nullable = false, length = 128) private String challengeKey;
    private Integer answer;
    @Column(nullable = false) private boolean passed;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
}
