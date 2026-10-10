package com.histar.be.chat.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "journey_memories", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "location_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JourneyMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "location_id", nullable = false)
    private UUID locationId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "last_visited_poi", length = 255)
    private String lastVisitedPoi;

    @Column(name = "journey_summary", length = 1000)
    private String journeySummary;

    @Column(length = 16)
    private String locale;

    @Column(name = "preferred_mode", length = 32)
    private String preferredMode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static JourneyMemory disabled(UUID userId, UUID locationId) {
        Instant now = Instant.now();
        return JourneyMemory.builder()
                .userId(userId)
                .locationId(locationId)
                .enabled(false)
                .expiresAt(now)
                .updatedAt(now)
                .build();
    }
}
