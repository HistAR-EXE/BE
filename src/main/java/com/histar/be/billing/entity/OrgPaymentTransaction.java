package com.histar.be.billing.entity;

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
@Table(name = "org_payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "org_name", nullable = false)
    private String orgName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "transfer_content", nullable = false, unique = true)
    private String transferContent;

    @Column(name = "amount_vnd", nullable = false)
    private Long amountVnd;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "return_to_path")
    private String returnToPath;

    @Column(name = "qr_url", columnDefinition = "text")
    private String qrUrl;

    @Column(name = "provider_transaction_id", unique = true)
    private Long providerTransactionId;

    @Column(name = "provider_reference_code")
    private String providerReferenceCode;

    @Column(name = "provider_gateway")
    private String providerGateway;

    @Column(name = "provider_payload", columnDefinition = "text")
    private String providerPayload;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
