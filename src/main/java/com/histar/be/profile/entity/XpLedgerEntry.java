package com.histar.be.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "xp_ledger", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_key"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class XpLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "event_key", nullable = false, length = 255) private String eventKey;
    @Column(nullable = false) private int points;
    @Column(name = "awarded_at", nullable = false) private Instant awardedAt;
}
