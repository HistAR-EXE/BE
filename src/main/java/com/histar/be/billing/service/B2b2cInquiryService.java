package com.histar.be.billing.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.billing.dto.AdminCreateB2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryResponse;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.common.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import com.histar.be.mail.HistarEmailService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.util.HtmlUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class B2b2cInquiryService {

    private static final int MAX_SUBMITS_PER_HOUR = 5;

    private final HeritageDigitizationInquiryRepository repository;
    private final HistarEmailService histarEmailService;
    private final HistarOrgProperties histarOrgProperties;
    private final AdminAuditService audit;
    private final CurrentUserAccessor currentUser;

    private final Map<String, List<Instant>> submitAttemptsByEmail = new ConcurrentHashMap<>();

    @Transactional
    public B2b2cInquiryResponse submit(B2b2cInquiryRequest request) {
        if (isHoneypotFilled(request)) {
            Instant now = Instant.now();
            return new B2b2cInquiryResponse(UUID.randomUUID(), "NEW", now);
        }

        String email = request.contactEmail().trim().toLowerCase();
        enforceSubmitRateLimit(email);

        Instant now = Instant.now();
        String interest = request.interestSiteCode() != null && !request.interestSiteCode().isBlank()
                ? request.interestSiteCode().trim().toLowerCase()
                : null;
        HeritageDigitizationInquiry saved = repository.save(HeritageDigitizationInquiry.builder()
                .siteName(request.siteName().trim())
                .organizationName(clean(request.organizationName()))
                .contactRole(clean(request.contactRole()))
                .siteLocation(clean(request.siteLocation()))
                .availableMaterials(clean(request.availableMaterials()))
                .targetStart(clean(request.targetStart()))
                .interestSiteCode(interest)
                .contactName(request.contactName().trim())
                .contactEmail(email)
                .contactPhone(request.contactPhone() != null ? request.contactPhone().trim() : null)
                .packageType(request.packageType().trim().toUpperCase())
                .message(request.message())
                .status("NEW")
                .createdAt(now)
                .updatedAt(now)
                .build());

        recordSubmitAttempt(email, now);
        sendInquiryEmails(saved);

        return new B2b2cInquiryResponse(saved.getId(), saved.getStatus(), saved.getCreatedAt());
    }

    /**
     * Admin CRM intake — no honeypot / rate limit; optional notes + initial stage; no outbound email.
     */
    @Transactional
    public B2b2cInquiryItem createAsAdmin(AdminCreateB2b2cInquiryRequest request) {
        UUID actorId = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        String packageType = request.packageType().trim().toUpperCase();
        if (!packageType.matches("^[A-Z0-9_]{2,32}$")) {
            throw new BusinessRuleException("packageType chỉ gồm A–Z, 0–9 và underscore (2–32 ký tự)");
        }
        String status = request.status() != null && !request.status().isBlank()
                ? request.status().trim().toUpperCase()
                : "NEW";
        Instant now = Instant.now();
        String interest = request.interestSiteCode() != null && !request.interestSiteCode().isBlank()
                ? request.interestSiteCode().trim().toLowerCase()
                : null;
        HeritageDigitizationInquiry saved = repository.save(HeritageDigitizationInquiry.builder()
                .siteName(request.siteName().trim())
                .interestSiteCode(interest)
                .contactName(request.contactName().trim())
                .contactEmail(request.contactEmail().trim().toLowerCase())
                .contactPhone(request.contactPhone() != null && !request.contactPhone().isBlank()
                        ? request.contactPhone().trim()
                        : null)
                .packageType(packageType)
                .message(request.message() != null && !request.message().isBlank() ? request.message().trim() : null)
                .status(status)
                .adminNotes(request.adminNotes() != null && !request.adminNotes().isBlank()
                        ? request.adminNotes().trim()
                        : null)
                .contactedAt("CONTACTED".equals(status)
                                || "QUALIFIED".equals(status)
                                || "WON".equals(status)
                        ? now
                        : null)
                .createdAt(now)
                .updatedAt(now)
                .build());
        audit.append(new AdminAuditCommand(
                actorId,
                "B2B2C_INQUIRY",
                saved.getId(),
                "CREATED",
                null,
                null,
                null,
                null,
                Map.of(
                        "siteName", saved.getSiteName(),
                        "packageType", saved.getPackageType(),
                        "status", saved.getStatus())));
        return toItem(saved);
    }

    @Transactional(readOnly = true)
    public List<B2b2cInquiryItem> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<B2b2cInquiryItem> listPaged(
            int page, int size, String status, String q, Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("from must be before or equal to to");
        }
        Specification<HeritageDigitizationInquiry> filters = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            if (q != null && !q.isBlank()) {
                String needle = "%" + q.trim().toLowerCase() + "%";
                List<Predicate> matches = new ArrayList<>(List.of(
                        cb.like(cb.lower(root.get("siteName")), needle),
                        cb.like(cb.lower(root.get("organizationName")), needle),
                        cb.like(cb.lower(root.get("contactName")), needle),
                        cb.like(cb.lower(root.get("contactEmail")), needle),
                        cb.like(cb.lower(root.get("contactPhone")), needle)));
                try {
                    matches.add(cb.equal(root.get("id"), UUID.fromString(q.trim())));
                } catch (IllegalArgumentException ignored) {
                    // Free-text search is still valid when the query is not a UUID.
                }
                predicates.add(cb.or(matches.toArray(Predicate[]::new)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        var result = repository.findAll(filters, PageRequest.of(
                Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.<B2b2cInquiryItem>builder()
                .items(result.getContent().stream().map(this::toItem).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalItems(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }

    @Transactional
    public B2b2cInquiryItem updateStatus(UUID id, String status, String adminNotes) {
        UUID actorId = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        HeritageDigitizationInquiry inquiry = repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + id));
        String oldStatus = inquiry.getStatus();
        String oldNotes = inquiry.getAdminNotes();
        String normalized = status.trim().toUpperCase();
        Instant now = Instant.now();
        inquiry.setStatus(normalized);
        inquiry.setUpdatedAt(now);
        if (adminNotes != null) {
            inquiry.setAdminNotes(adminNotes.isBlank() ? null : adminNotes.trim());
        }
        if ("CONTACTED".equals(normalized) && inquiry.getContactedAt() == null) {
            inquiry.setContactedAt(now);
        }
        HeritageDigitizationInquiry saved = repository.save(inquiry);
        audit.append(new AdminAuditCommand(
                actorId,
                "B2B2C_INQUIRY",
                id,
                "STATUS_UPDATED",
                null,
                null,
                null,
                null,
                Map.of(
                        "oldStatus", oldStatus == null ? "" : oldStatus,
                        "newStatus", normalized,
                        "notesChanged", !java.util.Objects.equals(oldNotes, saved.getAdminNotes()),
                        "notesLength", saved.getAdminNotes() == null ? 0 : saved.getAdminNotes().length())));
        return toItem(saved);
    }

    private boolean isHoneypotFilled(B2b2cInquiryRequest request) {
        return isNonBlank(request.website()) || isNonBlank(request.companyUrl());
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void enforceSubmitRateLimit(String email) {
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        List<Instant> recent = submitAttemptsByEmail.compute(email, (key, attempts) -> {
            List<Instant> list = attempts != null ? new ArrayList<>(attempts) : new ArrayList<>();
            list.removeIf(ts -> ts.isBefore(hourAgo));
            return list;
        });
        if (recent.size() >= MAX_SUBMITS_PER_HOUR) {
            throw new BusinessRuleException("Quá nhiều yêu cầu tư vấn. Vui lòng thử lại sau một giờ.");
        }
    }

    private void recordSubmitAttempt(String email, Instant at) {
        submitAttemptsByEmail.compute(email, (key, attempts) -> {
            List<Instant> list = attempts != null ? new ArrayList<>(attempts) : new ArrayList<>();
            list.add(at);
            return list;
        });
    }

    private void sendInquiryEmails(HeritageDigitizationInquiry inquiry) {
        String customerHtml =
                """
                <p>Xin chào %s,</p>
                <p>TimeLens đã nhận yêu cầu số hóa di sản cho <strong>%s</strong> (gói %s).</p>
                <p>Đội ngũ sales sẽ liên hệ qua email hoặc số điện thoại bạn đã cung cấp trong thời gian sớm nhất.</p>
                """
                        .formatted(
                                html(inquiry.getContactName()),
                                html(inquiry.getSiteName()),
                                html(inquiry.getPackageType()));
        try {
            histarEmailService.sendHtml(
                    inquiry.getContactEmail(), "TimeLens — Đã nhận yêu cầu tư vấn B2G", customerHtml);
        } catch (RuntimeException ex) {
            log.warn("B2B2C inquiry confirmation email failed for {}: {}", inquiry.getContactEmail(), ex.getMessage());
        }

        String salesInbox = histarOrgProperties.getSalesInbox();
        if (salesInbox == null || salesInbox.isBlank()) {
            log.info(
                    "histar.sales-inbox not configured — skip sales notify for inquiry {}",
                    inquiry.getId());
            return;
        }
        String salesHtml =
                """
                <p>Yêu cầu B2G mới:</p>
                <ul>
                <li>Đơn vị: %s</li>
                <li>Site: %s</li>
                <li>Địa điểm: %s</li>
                <li>Liên hệ: %s (%s)</li>
                <li>Chức vụ: %s</li>
                <li>Phone: %s</li>
                <li>Gói: %s</li>
                <li>Thời điểm dự kiến: %s</li>
                <li>Tư liệu sẵn có: %s</li>
                <li>Nhu cầu: %s</li>
                <li>ID: %s</li>
                </ul>
                """
                        .formatted(
                                html(inquiry.getOrganizationName()),
                                html(inquiry.getSiteName()),
                                html(inquiry.getSiteLocation()),
                                html(inquiry.getContactName()),
                                html(inquiry.getContactEmail()),
                                html(inquiry.getContactRole()),
                                html(inquiry.getContactPhone()),
                                html(inquiry.getPackageType()),
                                html(inquiry.getTargetStart()),
                                html(inquiry.getAvailableMaterials()),
                                html(inquiry.getMessage()),
                                inquiry.getId());
        try {
            histarEmailService.sendHtml(salesInbox.trim(), "TimeLens — Yêu cầu tư vấn B2G mới", salesHtml);
        } catch (RuntimeException ex) {
            log.warn("B2B2C sales notify email failed for inquiry {}: {}", inquiry.getId(), ex.getMessage());
        }
    }

    private static String html(String value) {
        return value == null || value.isBlank() ? "—" : HtmlUtils.htmlEscape(value);
    }

    private B2b2cInquiryItem toItem(HeritageDigitizationInquiry inquiry) {
        return new B2b2cInquiryItem(
                inquiry.getId(),
                inquiry.getSiteName(),
                inquiry.getContactName(),
                inquiry.getContactEmail(),
                inquiry.getContactPhone(),
                inquiry.getPackageType(),
                inquiry.getMessage(),
                inquiry.getStatus(),
                inquiry.getAdminNotes(),
                inquiry.getContactedAt(),
                inquiry.getCreatedAt(),
                inquiry.getInterestSiteCode(),
                inquiry.getOrganizationId(),
                inquiry.getOrganizationName(),
                inquiry.getContactRole(),
                inquiry.getSiteLocation(),
                inquiry.getAvailableMaterials(),
                inquiry.getTargetStart());
    }
}
