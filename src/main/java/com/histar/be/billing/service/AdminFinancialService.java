package com.histar.be.billing.service;

import com.histar.be.billing.dto.AdminBillingOverviewResponse;
import com.histar.be.billing.dto.MrrCoverage;
import com.histar.be.billing.dto.PlanRevenuePoint;
import com.histar.be.billing.dto.RevenueTrendPoint;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.entity.OrgBillingSubscription;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.billing.repository.OrgBillingSubscriptionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminFinancialService {
    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final B2cPaymentTransactionRepository b2cPayments;
    private final OrgPaymentTransactionRepository orgPayments;
    private final B2cSubscriptionRepository b2cSubscriptions;
    private final OrgBillingSubscriptionRepository orgSubscriptions;
    private final Clock clock;

    @Autowired
    public AdminFinancialService(
            B2cPaymentTransactionRepository b2cPayments,
            OrgPaymentTransactionRepository orgPayments,
            B2cSubscriptionRepository b2cSubscriptions,
            OrgBillingSubscriptionRepository orgSubscriptions) {
        this(b2cPayments, orgPayments, b2cSubscriptions, orgSubscriptions, Clock.system(VN_ZONE));
    }

    public AdminFinancialService(
            B2cPaymentTransactionRepository b2cPayments,
            OrgPaymentTransactionRepository orgPayments,
            B2cSubscriptionRepository b2cSubscriptions,
            OrgBillingSubscriptionRepository orgSubscriptions,
            Clock clock) {
        this.b2cPayments = b2cPayments;
        this.orgPayments = orgPayments;
        this.b2cSubscriptions = b2cSubscriptions;
        this.orgSubscriptions = orgSubscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AdminBillingOverviewResponse overview(int requestedMonths) {
        int months = Math.min(36, Math.max(1, requestedMonths));
        Instant now = clock.instant();
        YearMonth current = YearMonth.now(clock);
        Instant currentStart = atStart(current);
        Instant previousStart = atStart(current.minusMonths(1));
        long currentRevenue = realized(currentStart, now);
        long previousRevenue = realized(previousStart, currentStart);
        Double mom = previousRevenue == 0 ? null : percentChange(currentRevenue, previousRevenue);
        String comparisonStatus = previousRevenue == 0 ? "NO_BASELINE" : "OK";

        YearMonth lastYear = current.minusYears(1);
        Instant lastYearStart = atStart(lastYear);
        Instant lastYearComparableEnd = now.atZone(VN_ZONE).minusYears(1).toInstant();
        long historyCount = b2cPayments.countByStatusIgnoreCaseAndPaidAtIsNotNullAndPaidAtBefore("PAID", lastYearComparableEnd)
                + orgPayments.countByStatusIgnoreCaseAndPaidAtIsNotNullAndPaidAtBefore("PAID", lastYearComparableEnd);
        Double yoy = null;
        String yoyStatus = "INSUFFICIENT_HISTORY";
        if (historyCount > 0) {
            long lastYearRevenue = realized(lastYearStart, lastYearComparableEnd);
            if (lastYearRevenue > 0) {
                yoy = percentChange(currentRevenue, lastYearRevenue);
                yoyStatus = "OK";
            } else {
                yoyStatus = "NO_BASELINE";
            }
        }

        List<RevenueTrendPoint> trend = new ArrayList<>();
        for (int offset = months - 1; offset >= 0; offset--) {
            YearMonth month = current.minusMonths(offset);
            Instant from = atStart(month);
            Instant to = month.equals(current) ? now : atStart(month.plusMonths(1));
            long b2c = b2cPayments.sumPaidAmountBetween(from, to);
            long b2b = orgPayments.sumPaidAmountBetween(from, to);
            trend.add(new RevenueTrendPoint(month.toString(), b2c, b2b, b2c + b2b));
        }

        MrrResult mrr = estimateMrr(LocalDate.now(clock));
        Double coverage = mrr.included + mrr.excluded == 0
                ? null
                : round(mrr.included * 100.0 / (mrr.included + mrr.excluded));
        Instant windowStart = atStart(current.minusMonths(months - 1L));
        List<PlanRevenuePoint> planRevenue = planRevenue(windowStart, now);
        return new AdminBillingOverviewResponse(
                currentRevenue, mom, comparisonStatus, yoy, yoyStatus,
                mrr.amount, mrr.amount * 12, List.copyOf(trend),
                new MrrCoverage(mrr.included, mrr.excluded, coverage),
                List.copyOf(planRevenue));
    }

    private List<PlanRevenuePoint> planRevenue(Instant from, Instant to) {
        Map<String, Long> buckets = new LinkedHashMap<>();
        long b2c = b2cPayments.sumPaidAmountBetween(from, to);
        if (b2c > 0) {
            buckets.put("Gói Cá nhân (B2C)", b2c);
        }
        for (Object[] row : orgPayments.sumPaidAmountGroupedByPlanTypeBetween(from, to)) {
            String planType = row[0] == null ? "UNKNOWN" : String.valueOf(row[0]);
            long amount = row[1] instanceof Number number ? number.longValue() : 0L;
            if (amount <= 0) {
                continue;
            }
            String label = labelForOrgPlan(planType);
            buckets.merge(label, amount, Long::sum);
        }
        List<PlanRevenuePoint> points = new ArrayList<>();
        for (Map.Entry<String, Long> entry : buckets.entrySet()) {
            points.add(new PlanRevenuePoint(
                    entry.getKey(),
                    PlanRevenuePoint.descriptionFor(entry.getKey()),
                    entry.getValue()));
        }
        return points;
    }

    private String labelForOrgPlan(String planType) {
        String key = planType == null ? "" : planType.trim().toUpperCase(Locale.ROOT);
        if (key.contains("EDU") || key.contains("SCHOOL") || key.contains("K12")
                || "STANDARD".equals(key) || "ORG_PRO".equals(key) || "PRO".equals(key)) {
            return "Gói Trường học (Edu)";
        }
        if (key.contains("MUSEUM") || key.contains("HERITAGE") || "ORG_BASIC".equals(key)
                || "MICRO".equals(key) || "BASIC".equals(key) || "PREMIUM".equals(key)) {
            return "Gói Bảo tàng / Tổ chức";
        }
        return "Gói Bảo tàng / Tổ chức";
    }

    private MrrResult estimateMrr(LocalDate today) {
        long amount = 0;
        int included = 0;
        int excluded = 0;
        for (B2cSubscription subscription : b2cSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(today)) {
            if (subscription.getPriceVnd() != null && isPaidMethod(subscription.getPaymentMethod())) {
                amount += subscription.getPriceVnd();
                included++;
            } else {
                excluded++;
            }
        }
        for (OrgBillingSubscription subscription : orgSubscriptions.findAllByIsActiveTrueAndEndDateGreaterThanEqual(today)) {
            Long normalized = normalizeOrgMrr(subscription);
            if (normalized == null) {
                excluded++;
            } else {
                amount += normalized;
                included++;
            }
        }
        return new MrrResult(amount, included, excluded);
    }

    private Long normalizeOrgMrr(OrgBillingSubscription subscription) {
        if (!isPaidMethod(subscription.getPaymentMethod())
                || subscription.getSourcePaymentTransactionId() == null
                || subscription.getBillingCadence() == null
                || subscription.getContractValueVnd() == null
                || subscription.getCurrency() == null
                || !"VND".equalsIgnoreCase(subscription.getCurrency())
                || subscription.getQuantity() == null) {
            return null;
        }
        return switch (subscription.getBillingCadence().toUpperCase(Locale.ROOT)) {
            case "MONTHLY" -> subscription.getContractValueVnd();
            case "QUARTERLY" -> subscription.getContractValueVnd() / 3;
            case "ANNUAL" -> subscription.getContractValueVnd() / 12;
            default -> null;
        };
    }

    private boolean isPaidMethod(String method) {
        return method != null && !"DEMO".equalsIgnoreCase(method) && !"TRIAL".equalsIgnoreCase(method);
    }

    private long realized(Instant from, Instant to) {
        return b2cPayments.sumPaidAmountBetween(from, to) + orgPayments.sumPaidAmountBetween(from, to);
    }

    private Instant atStart(YearMonth month) {
        return month.atDay(1).atStartOfDay(VN_ZONE).toInstant();
    }

    private Double percentChange(long current, long baseline) {
        return round((current - baseline) * 100.0 / baseline);
    }

    private Double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record MrrResult(long amount, int included, int excluded) {}
}
