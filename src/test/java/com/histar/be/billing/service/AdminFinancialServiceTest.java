package com.histar.be.billing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminFinancialServiceTest {

    @Mock B2cPaymentTransactionRepository b2cPayments;
    @Mock OrgPaymentTransactionRepository orgPayments;
    @Mock B2cSubscriptionRepository b2cSubscriptions;
    @Mock OrgBillingSubscriptionRepository orgSubscriptions;

    @Test
    void reportsNoBaselineAndKeepsChannelSplitInSixMonthTrend() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        when(b2cPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(orgPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(b2cPayments.sumPaidAmountBetween(
                Instant.parse("2026-09-30T17:00:00Z"), Instant.parse("2026-10-15T03:00:00Z")))
                .thenReturn(490_000L);
        when(orgPayments.sumPaidAmountBetween(
                Instant.parse("2026-09-30T17:00:00Z"), Instant.parse("2026-10-15T03:00:00Z")))
                .thenReturn(15_000_000L);
        when(b2cSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(any())).thenReturn(List.of());
        when(orgSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(any())).thenReturn(List.of());
        when(orgPayments.sumPaidAmountGroupedByPlanTypeBetween(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[] {"STANDARD", 15_000_000L}));

        var result = service(clock).overview(6);

        assertThat(result.currentMonthRevenue()).isEqualTo(15_490_000L);
        assertThat(result.revenueMomPercent()).isNull();
        assertThat(result.comparisonStatus()).isEqualTo("NO_BASELINE");
        assertThat(result.revenueTrend()).hasSize(6);
        assertThat(result.revenueTrend().get(5).b2cRevenue()).isEqualTo(490_000L);
        assertThat(result.revenueTrend().get(5).b2bRevenue()).isEqualTo(15_000_000L);
        assertThat(result.planRevenue()).isNotEmpty();
    }

    @Test
    void returnsNoMonthlyRevenueSeriesWhenEveryRequestedBucketIsZero() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        when(b2cPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(orgPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(b2cSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(any())).thenReturn(List.of());
        when(orgSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(any())).thenReturn(List.of());
        when(orgPayments.sumPaidAmountGroupedByPlanTypeBetween(any(), any())).thenReturn(List.of());

        var result = service(clock).overview(6);

        assertThat(result.revenueTrend()).isEmpty();
        assertThat(result.revenueMomPercent()).isNull();
    }

    @Test
    void normalizesEligibleRecurringContractsAndReportsCoverage() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-15T03:00:00Z"), ZoneId.of("Asia/Ho_Chi_Minh"));
        when(b2cPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(orgPayments.sumPaidAmountBetween(any(), any())).thenReturn(0L);
        when(b2cSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(LocalDate.of(2026, 10, 15)))
                .thenReturn(List.of(
                        B2cSubscription.builder().priceVnd(49_000).paymentMethod("SEPAY").build(),
                        B2cSubscription.builder().priceVnd(49_000).paymentMethod("DEMO").build()));
        when(orgSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(LocalDate.of(2026, 10, 15)))
                .thenReturn(List.of(
                        recurring("ANNUAL", 120_000_000L, "SEPAY"),
                        recurring("QUARTERLY", 30_000_000L, "BANK"),
                        recurring("MONTHLY", 2_000_000L, "SEPAY"),
                        recurring("ONE_TIME", 8_000_000L, "SEPAY"),
                        recurring(null, null, "SEPAY")));
        when(orgPayments.sumPaidAmountGroupedByPlanTypeBetween(any(), any())).thenReturn(List.of());

        var result = service(clock).overview(6);

        assertThat(result.estimatedMrr()).isEqualTo(22_049_000L);
        assertThat(result.estimatedArr()).isEqualTo(264_588_000L);
        assertThat(result.mrrCoverage().includedCount()).isEqualTo(4);
        assertThat(result.mrrCoverage().excludedCount()).isEqualTo(3);
        assertThat(result.mrrCoverage().coveragePercent()).isEqualTo(57.1);
    }

    private AdminFinancialService service(Clock clock) {
        return new AdminFinancialService(b2cPayments, orgPayments, b2cSubscriptions, orgSubscriptions, clock);
    }

    private OrgBillingSubscription recurring(String cadence, Long value, String method) {
        return OrgBillingSubscription.builder()
                .sourcePaymentTransactionId(value == null ? null : java.util.UUID.randomUUID())
                .billingCadence(cadence).contractValueVnd(value).currency("VND").quantity(1)
                .paymentMethod(method).build();
    }
}
