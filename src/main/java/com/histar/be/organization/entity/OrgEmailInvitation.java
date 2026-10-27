package com.histar.be.organization.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "organization_email_invitations")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrgEmailInvitation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false) private String email;
    @Column(name = "invited_by", nullable = false) private UUID invitedBy;
    @Column(nullable = false) private String status;
    @Column(name = "delivery_status", nullable = false) private String deliveryStatus;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "accepted_at") private Instant acceptedAt;
}
