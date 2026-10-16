package com.histar.be.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgBillingSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "plan_type", nullable = false)
    private String planType;

    @Column(name = "price_vnd", nullable = false)
    private Long priceVnd;

    @Column(name = "source_payment_transaction_id")
    private UUID sourcePaymentTransactionId;

    @Column(name = "billing_cadence")
    private String billingCadence;

    @Column(name = "contract_value_vnd")
    private Long contractValueVnd;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "created_at")
    private Instant createdAt;
}
