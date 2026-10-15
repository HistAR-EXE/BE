package com.histar.be.billing.dto;

import java.util.List;

/** FE Admin Billing/Dashboard contract (field names consumed by HistAR FE). */
public record AdminBillingOverviewUiResponse(
        long actualRevenueMonthVnd,
        Long previousMonthRevenueVnd,
        Double monthOverMonthPct,
        long estimatedMrrVnd,
        long estimatedArrVnd,
        Double revenueCoveragePct,
        long activePaidSubscriptions,
        String updatedAt,
        List<AdminRevenueMonthUi> monthlyRevenue,
        List<AdminPlanRevenueUi> planRevenue) {

    public record AdminRevenueMonthUi(String month, long b2cRevenueVnd, long b2bRevenueVnd) {}

    public record AdminPlanRevenueUi(String plan, String description, long revenueVnd) {
        public AdminPlanRevenueUi(String plan, long revenueVnd) {
            this(plan, PlanRevenuePoint.descriptionFor(plan), revenueVnd);
        }
    }

    public static AdminBillingOverviewUiResponse from(AdminBillingOverviewResponse source, String updatedAt) {
        List<AdminRevenueMonthUi> monthly = source.revenueTrend().stream()
                .map(point -> new AdminRevenueMonthUi(point.month(), point.b2cRevenue(), point.b2bRevenue()))
                .toList();
        List<AdminPlanRevenueUi> plans = source.planRevenue() == null || source.planRevenue().isEmpty()
                ? planRevenueFromMonthly(monthly)
                : source.planRevenue().stream()
                        .map(point -> new AdminPlanRevenueUi(
                                point.plan(),
                                point.description() == null || point.description().isBlank()
                                        ? PlanRevenuePoint.descriptionFor(point.plan())
                                        : point.description(),
                                point.revenueVnd()))
                        .toList();
        long activePaid = source.mrrCoverage() == null ? 0L : source.mrrCoverage().includedCount();
        Double coverage = source.mrrCoverage() == null ? null : source.mrrCoverage().coveragePercent();
        Long previousMonth = null;
        if (source.revenueMomPercent() != null && source.currentMonthRevenue() > 0) {
            double factor = 1.0 + (source.revenueMomPercent() / 100.0);
            if (factor != 0) {
                previousMonth = Math.round(source.currentMonthRevenue() / factor);
            }
        }
        return new AdminBillingOverviewUiResponse(
                source.currentMonthRevenue(),
                previousMonth,
                source.revenueMomPercent(),
                source.estimatedMrr(),
                source.estimatedArr(),
                coverage,
                activePaid,
                updatedAt,
                monthly,
                plans);
    }

    /** Fallback when financial service has not populated plan buckets yet. */
    static List<AdminPlanRevenueUi> planRevenueFromMonthly(List<AdminRevenueMonthUi> monthly) {
        long b2c = monthly.stream().mapToLong(AdminRevenueMonthUi::b2cRevenueVnd).sum();
        long b2b = monthly.stream().mapToLong(AdminRevenueMonthUi::b2bRevenueVnd).sum();
        java.util.ArrayList<AdminPlanRevenueUi> plans = new java.util.ArrayList<>();
        if (b2c > 0) {
            plans.add(new AdminPlanRevenueUi("Gói Cá nhân (B2C)", b2c));
        }
        if (b2b > 0) {
            // Without plan_type split, attribute B2B to org segment (honest aggregate).
            plans.add(new AdminPlanRevenueUi("Gói Bảo tàng / Tổ chức", b2b));
        }
        return List.copyOf(plans);
    }
}
