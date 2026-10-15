package com.histar.be.billing.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdminBillingOverviewUiResponseTest {

    @Test
    void mapsPlanRevenueWithDescriptionsForAdminDonut() {
        var source = new AdminBillingOverviewResponse(
                100L,
                10.0,
                "OK",
                null,
                "INSUFFICIENT_HISTORY",
                50L,
                600L,
                List.of(new RevenueTrendPoint("2026-10", 40L, 60L, 100L)),
                new MrrCoverage(2, 1, 66.7),
                List.of(
                        new PlanRevenuePoint("Gói Cá nhân (B2C)", 857_780_000L),
                        new PlanRevenuePoint("Gói Trường học (Edu)", 1_102_800_000L),
                        new PlanRevenuePoint("Gói Bảo tàng / Tổ chức", 490_100_000L)));

        var ui = AdminBillingOverviewUiResponse.from(source, "2026-10-15T10:00:00Z");

        assertThat(ui.planRevenue()).hasSize(3);
        assertThat(ui.planRevenue().get(0).plan()).isEqualTo("Gói Cá nhân (B2C)");
        assertThat(ui.planRevenue().get(0).description()).isEqualTo("Quest vé lẻ & Plus");
        assertThat(ui.planRevenue().get(0).revenueVnd()).isEqualTo(857_780_000L);
        assertThat(ui.planRevenue().get(1).description()).isEqualTo("Bản quyền ngoại khóa K12");
        assertThat(ui.planRevenue().get(2).description()).isEqualTo("Hợp tác địa phương");
    }

    @Test
    void fallsBackToChannelTotalsWhenPlanRevenueMissing() {
        var source = new AdminBillingOverviewResponse(
                100L,
                null,
                "NO_BASELINE",
                null,
                "INSUFFICIENT_HISTORY",
                0L,
                0L,
                List.of(
                        new RevenueTrendPoint("2026-09", 100L, 200L, 300L),
                        new RevenueTrendPoint("2026-10", 300L, 100L, 400L)),
                new MrrCoverage(0, 0, null),
                List.of());

        var ui = AdminBillingOverviewUiResponse.from(source, "now");

        assertThat(ui.planRevenue()).extracting(AdminBillingOverviewUiResponse.AdminPlanRevenueUi::plan)
                .containsExactly("Gói Cá nhân (B2C)", "Gói Bảo tàng / Tổ chức");
        assertThat(ui.planRevenue().get(0).revenueVnd()).isEqualTo(400L);
        assertThat(ui.planRevenue().get(1).revenueVnd()).isEqualTo(300L);
    }
}
