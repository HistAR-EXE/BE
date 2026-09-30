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
@Table(name = "b2c_payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class B2cPaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "order_code", nullable = false, unique = true)
    private String orderCode;

    @Column(name = "transfer_content", nullable = false, unique = true)
    private String transferContent;

    @Column(name = "amount_vnd", nullable = false)
    private Integer amountVnd;

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

    /** b2c_subscriptions row activated by this payment (set after PAID). */
    @Column(name = "subscription_id")
    private UUID subscriptionId;

    /** PREMIUM (monthly) or JOURNEY_PASS (72h site unlock). */
    @Column(name = "plan_type", nullable = false, length = 32)
    @Builder.Default
    private String planType = "PREMIUM";

    /** Site slug for JOURNEY_PASS (cu-chi, …). */
    @Column(name = "site_code", length = 64)
    private String siteCode;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
