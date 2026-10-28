package com.histar.be.billing.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="ai_quota_reservations") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AiQuotaReservation {
 @Id private UUID id;
 @Column(nullable=false) private UUID userId;
 private UUID organizationId;
 @Column(nullable=false) private LocalDate quotaDate;
 @Column(nullable=false,length=12) private String status;
 @Column(nullable=false) private Instant expiresAt;
}
