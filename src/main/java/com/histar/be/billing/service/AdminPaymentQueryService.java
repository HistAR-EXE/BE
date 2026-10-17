package com.histar.be.billing.service;

import com.histar.be.billing.dto.AdminPaymentItem;
import com.histar.be.billing.repository.B2cPaymentTransactionRepository;
import com.histar.be.billing.repository.OrgPaymentTransactionRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPaymentQueryService {
    private static final int CSV_EXPORT_CAP = 10_000;
    private final B2cPaymentTransactionRepository b2cPayments;
    private final OrgPaymentTransactionRepository orgPayments;
    private final ProfileRepository profiles;

    @Transactional(readOnly = true)
    public PageResponse<AdminPaymentItem> search(
            int page, int size, String channel, String status, Instant from, Instant to, String sort) {
        List<AdminPaymentItem> filtered = filteredItems(channel, status, from, to, sort);
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        int start = Math.min(safePage * safeSize, filtered.size());
        int end = Math.min(start + safeSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (filtered.size() + safeSize - 1) / safeSize;
        return PageResponse.<AdminPaymentItem>builder()
                .items(filtered.subList(start, end)).page(safePage).size(safeSize)
                .totalItems(filtered.size()).totalPages(totalPages).build();
    }

    @Transactional(readOnly = true)
    public String exportCsv(String channel, String status, Instant from, Instant to, String sort) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("from must be before or equal to to");
        }
        String normalizedChannel = normalize(channel);
        String normalizedStatus = normalize(status);
        long b2cCount = normalizedChannel == null || "B2C".equals(normalizedChannel)
                ? b2cPayments.countForAdminExport(normalizedStatus, from, to) : 0;
        long b2bCount = normalizedChannel == null || "B2B".equals(normalizedChannel)
                ? orgPayments.countForAdminExport(normalizedStatus, from, to) : 0;
        if (b2cCount + b2bCount > CSV_EXPORT_CAP) {
            throw new BusinessRuleException("CSV export vượt quá giới hạn 10,000 bản ghi");
        }
        List<AdminPaymentItem> items = new ArrayList<>();
        if (b2cCount > 0) {
            items.addAll(toB2cItems(b2cPayments.findForAdminExport(
                    normalizedStatus, from, to, exportPage(sort, (int) b2cCount))));
        }
        if (b2bCount > 0) {
            items.addAll(toB2bItems(orgPayments.findForAdminExport(
                    normalizedStatus, from, to, exportPage(sort, (int) b2bCount))));
        }
        items.sort(comparator(sort));
        StringBuilder csv = new StringBuilder(
                "id,channel,orderCode,status,payerEmail,amountVnd,planType,organizationName,paidAt,createdAt\r\n");
        for (AdminPaymentItem item : items) {
            csv.append(csv(item.id())).append(',')
                    .append(csv(item.channel())).append(',')
                    .append(csv(item.orderCode())).append(',')
                    .append(csv(item.status())).append(',')
                    .append(csv(item.payerEmail())).append(',')
                    .append(item.amountVnd()).append(',')
                    .append(csv(item.planType())).append(',')
                    .append(csv(item.organizationName())).append(',')
                    .append(csv(item.paidAt())).append(',')
                    .append(csv(item.createdAt())).append("\r\n");
        }
        return csv.toString();
    }

    private List<AdminPaymentItem> toB2cItems(List<com.histar.be.billing.entity.B2cPaymentTransaction> transactions) {
        List<UUID> userIds = transactions.stream().map(t -> t.getUserId()).distinct().toList();
        Map<UUID, Profile> payerProfiles = profiles.findAllById(userIds).stream()
                .collect(Collectors.toMap(Profile::getId, Function.identity()));
        return transactions.stream().map(tx -> new AdminPaymentItem(
                tx.getId(), "B2C", tx.getOrderCode(), tx.getStatus(),
                payerProfiles.containsKey(tx.getUserId()) ? payerProfiles.get(tx.getUserId()).getEmail() : null,
                tx.getAmountVnd().longValue(), tx.getPlanType(), null, tx.getPaidAt(), tx.getCreatedAt())).toList();
    }

    private List<AdminPaymentItem> toB2bItems(List<com.histar.be.billing.entity.OrgPaymentTransaction> transactions) {
        return transactions.stream().map(tx -> new AdminPaymentItem(
                tx.getId(), "B2B", tx.getOrderCode(), tx.getStatus(), tx.getContactEmail(),
                tx.getAmountVnd(), tx.getPlanType(), tx.getOrgName(), tx.getPaidAt(), tx.getCreatedAt())).toList();
    }

    private PageRequest exportPage(String sort, int size) {
        String[] parts = (sort == null || sort.isBlank() ? "createdAt,desc" : sort).split(",", 2);
        String property = switch (parts[0]) {
            case "amountVnd", "paidAt", "status", "createdAt" -> parts[0];
            default -> "createdAt";
        };
        Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(0, Math.max(1, size), Sort.by(direction, property));
    }

    private List<AdminPaymentItem> filteredItems(
            String channel, String status, Instant from, Instant to, String sort) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("from must be before or equal to to");
        }
        String normalizedChannel = normalize(channel);
        String normalizedStatus = normalize(status);
        List<AdminPaymentItem> items = new ArrayList<>();
        if (normalizedChannel == null || "B2C".equals(normalizedChannel)) {
            var transactions = b2cPayments.findAll();
            List<UUID> userIds = transactions.stream().map(t -> t.getUserId()).distinct().toList();
            Map<UUID, Profile> payerProfiles = profiles.findAllById(userIds).stream()
                    .collect(Collectors.toMap(Profile::getId, Function.identity()));
            transactions.forEach(tx -> items.add(new AdminPaymentItem(
                    tx.getId(), "B2C", tx.getOrderCode(), tx.getStatus(),
                    payerProfiles.containsKey(tx.getUserId()) ? payerProfiles.get(tx.getUserId()).getEmail() : null,
                    tx.getAmountVnd().longValue(), tx.getPlanType(), null, tx.getPaidAt(), tx.getCreatedAt())));
        }
        if (normalizedChannel == null || "B2B".equals(normalizedChannel)) {
            orgPayments.findAll().forEach(tx -> items.add(new AdminPaymentItem(
                    tx.getId(), "B2B", tx.getOrderCode(), tx.getStatus(), tx.getContactEmail(),
                    tx.getAmountVnd(), tx.getPlanType(), tx.getOrgName(), tx.getPaidAt(), tx.getCreatedAt())));
        }
        return items.stream()
                .filter(item -> normalizedStatus == null || normalizedStatus.equals(normalize(item.status())))
                .filter(item -> from == null || !paymentTime(item).isBefore(from))
                .filter(item -> to == null || !paymentTime(item).isAfter(to))
                .sorted(comparator(sort))
                .toList();
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (value instanceof String && !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\r') >= 0 || text.indexOf('\n') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private Comparator<AdminPaymentItem> comparator(String sort) {
        String[] parts = (sort == null || sort.isBlank() ? "createdAt,desc" : sort).split(",", 2);
        Comparator<AdminPaymentItem> comparator = switch (parts[0]) {
            case "amountVnd" -> Comparator.comparingLong(AdminPaymentItem::amountVnd);
            case "paidAt" -> Comparator.comparing(AdminPaymentItem::paidAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "status" -> Comparator.comparing(AdminPaymentItem::status, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            default -> Comparator.comparing(AdminPaymentItem::createdAt, Comparator.nullsLast(Comparator.naturalOrder()));
        };
        return parts.length > 1 && "asc".equalsIgnoreCase(parts[1]) ? comparator : comparator.reversed();
    }

    private Instant paymentTime(AdminPaymentItem item) {
        return item.paidAt() != null ? item.paidAt() : item.createdAt();
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
