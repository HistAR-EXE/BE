package com.histar.be.admin.contentreport;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminContentReportService {
    private final ContentReportCaseRepository cases;
    private final CurrentUserAccessor currentUser;
    private final AdminAuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<ContentReportResponse> list(
            int page, int size, ContentReportStatus status, String q, Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessRuleException("from must be before or equal to to");
        }
        Specification<ContentReportCase> filters = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("event").get("occurredAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("event").get("occurredAt"), to));
            if (q != null && !q.isBlank()) {
                String needle = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("event").get("stationCode")), needle),
                        cb.like(cb.lower(root.get("event").get("payloadJson")), needle),
                        cb.like(cb.lower(root.get("adminNote")), needle)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ContentReportCase> result = cases.findAll(filters, PageRequest.of(
                Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "event.occurredAt")));
        return PageResponse.<ContentReportResponse>builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber()).size(result.getSize())
                .totalItems(result.getTotalElements()).totalPages(result.getTotalPages()).build();
    }

    @Transactional
    public ContentReportResponse update(UUID eventId, UpdateContentReportRequest request) {
        ContentReportCase report = cases.findByIdForUpdate(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Content report not found: " + eventId));
        if (!Objects.equals(report.getRowVersion(), request.rowVersion())) {
            throw new NarrativeConflictException(
                    ErrorCode.VERSION_CONFLICT, "Content report was changed by another administrator");
        }
        UUID actorId = currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        ContentReportStatus previousStatus = report.getStatus();
        Long previousVersion = report.getRowVersion();
        Instant now = Instant.now();
        report.setStatus(request.status());
        report.setAdminNote(normalizeNote(request.adminNote()));
        report.setReviewedBy(actorId);
        report.setReviewedAt(now);
        report.setUpdatedAt(now);
        ContentReportCase saved = cases.saveAndFlush(report);
        audit.append(new AdminAuditCommand(
                actorId, "CONTENT_REPORT", eventId, "CONTENT_REPORT_STATUS_UPDATED",
                previousVersion, saved.getRowVersion(), saved.getAdminNote(), null,
                Map.of("oldStatus", previousStatus.name(), "newStatus", saved.getStatus().name())));
        return toResponse(saved);
    }

    private String normalizeNote(String note) {
        return note == null || note.isBlank() ? null : note.trim();
    }

    private ContentReportResponse toResponse(ContentReportCase report) {
        var event = report.getEvent();
        return new ContentReportResponse(
                event.getId(), event.getStationCode(), event.getSessionId(), event.getPayloadJson(), event.getOccurredAt(),
                report.getStatus(), report.getAdminNote(), report.getReviewedBy(), report.getReviewedAt(),
                report.getRowVersion(), report.getCreatedAt(), report.getUpdatedAt());
    }
}
