package com.histar.be.billing.service;

import com.histar.be.billing.dto.B2b2cInquiryItem;
import com.histar.be.billing.dto.B2b2cInquiryRequest;
import com.histar.be.billing.dto.B2b2cInquiryResponse;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class B2b2cInquiryService {

    private static final int MAX_SUBMITS_PER_HOUR = 5;

    private final HeritageDigitizationInquiryRepository repository;
    private final HistarEmailService histarEmailService;
    private final HistarOrgProperties histarOrgProperties;

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

    @Transactional(readOnly = true)
    public List<B2b2cInquiryItem> listAll() {
        return repository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toItem)
                .toList();
    }

    @Transactional
    public B2b2cInquiryItem updateStatus(UUID id, String status, String adminNotes) {
        HeritageDigitizationInquiry inquiry = repository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + id));
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
        return toItem(repository.save(inquiry));
    }

    private boolean isHoneypotFilled(B2b2cInquiryRequest request) {
        return isNonBlank(request.website()) || isNonBlank(request.companyUrl());
    }

    private static boolean isNonBlank(String value) {
        return value != null && !value.isBlank();
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
                                inquiry.getContactName(),
                                inquiry.getSiteName(),
                                inquiry.getPackageType());
        try {
            histarEmailService.sendHtml(
                    inquiry.getContactEmail(), "TimeLens — Đã nhận yêu cầu tư vấn B2B2C", customerHtml);
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
                <p>Yêu cầu B2B2C mới:</p>
                <ul>
                <li>Site: %s</li>
                <li>Liên hệ: %s (%s)</li>
                <li>Phone: %s</li>
                <li>Gói: %s</li>
                <li>ID: %s</li>
                </ul>
                """
                        .formatted(
                                inquiry.getSiteName(),
                                inquiry.getContactName(),
                                inquiry.getContactEmail(),
                                inquiry.getContactPhone() != null ? inquiry.getContactPhone() : "—",
                                inquiry.getPackageType(),
                                inquiry.getId());
        try {
            histarEmailService.sendHtml(salesInbox.trim(), "TimeLens — B2B2C inquiry mới", salesHtml);
        } catch (RuntimeException ex) {
            log.warn("B2B2C sales notify email failed for inquiry {}: {}", inquiry.getId(), ex.getMessage());
        }
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
                inquiry.getInterestSiteCode());
    }
}
