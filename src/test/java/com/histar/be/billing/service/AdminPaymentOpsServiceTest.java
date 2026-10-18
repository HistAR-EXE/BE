package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.entity.B2cPaymentTransaction;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.B2cVisitEntitlement;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.entity.OrgPaymentTransaction;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.B2cVisitEntitlementRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPaymentOpsServiceTest {
    @Mock B2cPaymentTransactionRepository b2cPayments;
    @Mock OrgPaymentTransactionRepository b2bPayments;
    @Mock SepayB2cPaymentService b2cFinalizer;
    @Mock SepayOrgPaymentService b2bFinalizer;
    @Mock B2cSubscriptionRepository b2cSubscriptions;
    @Mock B2cVisitEntitlementRepository visitEntitlements;
    @Mock OrgBillingSubscriptionRepository orgSubscriptions;
    @Mock ProfileRepository profiles;
    @Mock OrganizationRepository organizations;
    @Mock AdminAuditService audit;
    @Mock CurrentUserAccessor currentUser;

    private AdminPaymentOpsService service;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        service = new AdminPaymentOpsService(
                b2cPayments, b2bPayments, b2cFinalizer, b2bFinalizer,
                b2cSubscriptions, visitEntitlements, orgSubscriptions,
                profiles, organizations, audit, currentUser);
        actorId = UUID.randomUUID();
        lenient().when(currentUser.getUserId()).thenReturn(Optional.of(actorId));
    }

    @Test
    void marksPendingB2cPaidThroughSharedWebhookFinalizerAndAudits() {
        B2cPaymentTransaction payment = b2cPayment("PENDING");
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(b2cPayments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.markPaid("B2C", payment.getId(), "Bank transfer verified");

        assertThat(result.status()).isEqualTo("PAID");
        assertThat(payment.getPaidAt()).isNotNull();
        verify(b2cFinalizer).finalizePaidB2c(payment, false);
        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(command.capture());
        assertThat(command.getValue().action()).isEqualTo("PAYMENT_MARKED_PAID");
        assertThat(command.getValue().entityType()).isEqualTo("B2C_PAYMENT");
        assertThat(command.getValue().idempotencyKey()).isEqualTo(payment.getId() + ":PAYMENT_MARKED_PAID");
        assertThat(command.getValue().reason()).isEqualTo("Bank transfer verified");
    }

    @Test
    void marksUnderpaidB2bPaidThroughSharedWebhookFinalizer() {
        OrgPaymentTransaction payment = b2bPayment("UNDERPAID");
        when(b2bPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(b2bPayments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.markPaid("B2B", payment.getId(), "Wire confirmed manually");

        assertThat(result.status()).isEqualTo("PAID");
        verify(b2bFinalizer).finalizePaidOrg(payment);
    }

    @Test
    void markPaidRecordsTheReceivedAmountInProviderPayload() {
        B2cPaymentTransaction payment = b2cPayment("UNDERPAID");
        payment.setProviderPayload("{\"id\":77,\"receivedAmount\":10000}");
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(b2cPayments.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markPaid("B2C", payment.getId(), "Manual bank confirmation");

        assertThat(payment.getProviderPayload()).contains("\"id\":77").contains("\"receivedAmount\":49000");
    }

    @Test
    void rejectsIllegalMarkPaidTransition() {
        B2cPaymentTransaction payment = b2cPayment("REFUNDED");
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.markPaid("B2C", payment.getId(), "Retry after refund"))
                .isInstanceOf(BusinessRuleException.class);

        verify(b2cFinalizer, never()).finalizePaidB2c(any(), any(Boolean.class));
        verify(audit, never()).append(any());
    }

    @Test
    void rejectsReasonShorterThanTenCharacters() {
        B2cPaymentTransaction payment = b2cPayment("PENDING");

        assertThatThrownBy(() -> service.voidPayment("B2C", payment.getId(), "too short"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void refundsPaidB2cAndRevokesOnlyItsLinkedSubscription() {
        UUID subscriptionId = UUID.randomUUID();
        B2cPaymentTransaction payment = b2cPayment("PAID");
        payment.setSubscriptionId(subscriptionId);
        B2cSubscription linked = B2cSubscription.builder()
                .id(subscriptionId).userId(payment.getUserId()).isActive(true).build();
        Profile profile = Profile.builder().id(payment.getUserId()).tier("PREMIUM").build();
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(b2cSubscriptions.findById(subscriptionId)).thenReturn(Optional.of(linked));
        when(b2cSubscriptions.findAllByUserIdOrderByCreatedAtDesc(payment.getUserId())).thenReturn(List.of(linked));
        when(profiles.findById(payment.getUserId())).thenReturn(Optional.of(profile));

        var result = service.refund("B2C", payment.getId(), "Customer refund approved");

        assertThat(result.status()).isEqualTo("REFUND");
        assertThat(linked.getIsActive()).isFalse();
        assertThat(profile.getTier()).isEqualTo("FREE");
        verify(b2cSubscriptions).save(linked);
        verify(audit).append(any(AdminAuditCommand.class));
    }

    @Test
    void refundsJourneyPassByDeletingOnlyEntitlementsForItsOrder() {
        B2cPaymentTransaction payment = b2cPayment("PAID");
        payment.setPlanType("JOURNEY_PASS");
        B2cVisitEntitlement entitlement = B2cVisitEntitlement.builder()
                .id(UUID.randomUUID()).userId(payment.getUserId()).expiresAt(Instant.now().plusSeconds(3600)).build();
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(visitEntitlements.findByOrderCode(payment.getOrderCode())).thenReturn(List.of(entitlement));

        service.refund("B2C", payment.getId(), "Journey pass refund");

        verify(visitEntitlements).deleteAll(List.of(entitlement));
        verify(b2cSubscriptions, never()).save(any());
    }

    @Test
    void rejectsJourneyPassRefundWhenPaymentOwnedEntitlementIsMissing() {
        B2cPaymentTransaction payment = b2cPayment("PAID");
        payment.setPlanType("JOURNEY_PASS");
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(visitEntitlements.findByOrderCode(payment.getOrderCode())).thenReturn(List.of());

        assertThatThrownBy(() -> service.refund("B2C", payment.getId(), "Missing entitlement refund"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entitlement");

        verify(audit, never()).append(any());
    }

    @Test
    void rejectsB2cRefundWhenLinkedSubscriptionBelongsToAnotherUser() {
        UUID subscriptionId = UUID.randomUUID();
        B2cPaymentTransaction payment = b2cPayment("PAID");
        payment.setSubscriptionId(subscriptionId);
        B2cSubscription foreignSubscription = B2cSubscription.builder()
                .id(subscriptionId).userId(UUID.randomUUID()).isActive(true).build();
        when(b2cPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(b2cSubscriptions.findById(subscriptionId)).thenReturn(Optional.of(foreignSubscription));

        assertThatThrownBy(() -> service.refund("B2C", payment.getId(), "Illegal subscription link"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("subscription");

        verify(b2cSubscriptions, never()).save(any());
        verify(audit, never()).append(any());
    }

    @Test
    void refundsPaidB2bAndExpiresOrgOnlyWhenLinkedSubscriptionWasLastActive() {
        UUID subscriptionId = UUID.randomUUID();
        UUID organizationId = UUID.randomUUID();
        OrgPaymentTransaction payment = b2bPayment("PAID");
        payment.setSubscriptionId(subscriptionId);
        payment.setOrganizationId(organizationId);
        OrgBillingSubscription linked = OrgBillingSubscription.builder()
                .id(subscriptionId).organizationId(organizationId).isActive(true).build();
        Organization organization = Organization.builder().id(organizationId).status("ACTIVE").build();
        when(b2bPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orgSubscriptions.findById(subscriptionId)).thenReturn(Optional.of(linked));
        when(orgSubscriptions.findAllByOrganizationIdOrderByCreatedAtDesc(organizationId)).thenReturn(List.of(linked));
        when(organizations.findById(organizationId)).thenReturn(Optional.of(organization));

        service.refund("B2B", payment.getId(), "Contract payment refunded");

        assertThat(linked.getIsActive()).isFalse();
        assertThat(organization.getStatus()).isEqualTo("EXPIRED");
        verify(orgSubscriptions).save(linked);
        verify(organizations).save(organization);
    }

    @Test
    void rejectsB2bRefundWhenPaymentOwnedSubscriptionIsMissing() {
        OrgPaymentTransaction payment = b2bPayment("PAID");
        payment.setSubscriptionId(UUID.randomUUID());
        when(b2bPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));
        when(orgSubscriptions.findById(payment.getSubscriptionId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refund("B2B", payment.getId(), "Missing subscription refund"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("subscription");

        verify(audit, never()).append(any());
    }

    @Test
    void voidsOnlyPendingOrUnderpaidPaymentsAndAudits() {
        OrgPaymentTransaction payment = b2bPayment("PENDING");
        when(b2bPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        var result = service.voidPayment("B2B", payment.getId(), "Duplicate invoice voided");

        assertThat(result.status()).isEqualTo("VOID");
        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(command.capture());
        assertThat(command.getValue().action()).isEqualTo("PAYMENT_VOIDED");
        assertThat(command.getValue().entityType()).isEqualTo("ORG_PAYMENT");
    }

    @Test
    void refundRequiresPaidStatus() {
        OrgPaymentTransaction payment = b2bPayment("PENDING");
        when(b2bPayments.findByIdForUpdate(payment.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.refund("B2B", payment.getId(), "Refund before payment"))
                .isInstanceOf(BusinessRuleException.class);
    }

    private B2cPaymentTransaction b2cPayment(String status) {
        return B2cPaymentTransaction.builder()
                .id(UUID.randomUUID()).userId(UUID.randomUUID()).orderCode("B2C-1")
                .amountVnd(49_000).status(status).planType("PREMIUM")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }

    private OrgPaymentTransaction b2bPayment(String status) {
        return OrgPaymentTransaction.builder()
                .id(UUID.randomUUID()).requesterUserId(UUID.randomUUID()).orderCode("B2B-1")
                .amountVnd(1_000_000L).status(status).planType("STANDARD")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
    }
}
