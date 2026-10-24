package com.histar.be.siteworkspace.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.siteworkspace.dto.CreateSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.SiteOnboardingResponse;
import com.histar.be.siteworkspace.entity.SiteMembership;
import com.histar.be.siteworkspace.entity.SiteOnboardingRequest;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.repository.SiteMembershipRepository;
import com.histar.be.siteworkspace.repository.SiteOnboardingRequestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SiteOnboardingService {

    private static final String DUPLICATE_PENDING_MESSAGE =
            "Đã có yêu cầu onboarding đang chờ duyệt cho site này";

    private static final Pattern EMBEDDED_SECRET = Pattern.compile(
            "-----BEGIN [A-Z0-9 ]*-----|data:[\\w.+-]+/[\\w.+-]+;base64,",
            Pattern.CASE_INSENSITIVE);

    private final SiteOnboardingRequestRepository siteOnboardingRequestRepository;
    private final SiteMembershipRepository siteMembershipRepository;
    private final LocationRepository locationRepository;
    private final AdminAuditService adminAuditService;

    @Transactional
    public SiteOnboardingResponse submit(UUID actorId, CreateSiteOnboardingRequest request) {
        requireActor(actorId);
        String representativeName = required(request.representativeName(), "Tên đại diện là bắt buộc");
        String organizationName = required(request.organizationName(), "Tên tổ chức là bắt buộc");
        String representativeEmail = required(request.representativeEmail(), "Email đại diện là bắt buộc");
        String evidenceNote = required(request.evidenceNote(), "Ghi chú xác minh là bắt buộc");
        String normalizedSiteCode = normalizeSiteCode(request.siteCode());
        Location location = locationRepository.findBySiteCodeIgnoreCase(normalizedSiteCode)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy site workspace"));
        requirePlainEvidence(evidenceNote);
        String siteCode = location.getSiteCode();
        if (siteOnboardingRequestRepository.existsByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
                actorId, siteCode, SiteOnboardingStatus.PENDING)) {
            throw new BusinessRuleException(DUPLICATE_PENDING_MESSAGE);
        }

        Instant now = Instant.now();
        try {
            SiteOnboardingRequest saved = siteOnboardingRequestRepository.save(SiteOnboardingRequest.builder()
                    .requesterId(actorId)
                    .siteCode(siteCode)
                    .representativeName(representativeName)
                    .organizationName(organizationName)
                    .representativeEmail(representativeEmail)
                    .evidenceNote(evidenceNote)
                    .status(SiteOnboardingStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
            siteOnboardingRequestRepository.flush();
            return toResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessRuleException(DUPLICATE_PENDING_MESSAGE);
        }
    }

    @Transactional(readOnly = true)
    public List<SiteOnboardingResponse> listMine(UUID actorId) {
        requireActor(actorId);
        return siteOnboardingRequestRepository.findByRequesterIdOrderByCreatedAtDesc(actorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SiteOnboardingResponse> listByStatus(SiteOnboardingStatus status) {
        return siteOnboardingRequestRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SiteOnboardingResponse approve(UUID requestId, UUID reviewerId, String note) {
        requireActor(reviewerId);
        SiteOnboardingRequest request = loadForUpdate(requestId);
        if (request.getStatus() == SiteOnboardingStatus.APPROVED) {
            return toResponse(request);
        }
        if (request.getStatus() != SiteOnboardingStatus.PENDING) {
            throw new BusinessRuleException("Yêu cầu onboarding không còn chờ duyệt");
        }
        Location location = locationRepository.findBySiteCodeIgnoreCase(request.getSiteCode())
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy site workspace"));
        String reviewNote = blankToNull(note);
        Instant reviewedAt = Instant.now();
        request.setStatus(SiteOnboardingStatus.APPROVED);
        request.setReviewedBy(reviewerId);
        request.setReviewNote(reviewNote);
        request.setReviewedAt(reviewedAt);
        request.setUpdatedAt(reviewedAt);
        SiteOnboardingRequest saved = siteOnboardingRequestRepository.save(request);
        grantSiteManager(location.getSiteCode(), saved.getRequesterId(), reviewerId);
        audit(reviewerId, saved, SiteOnboardingStatus.PENDING, "APPROVED", reviewNote);
        return toResponse(saved);
    }

    @Transactional
    public SiteOnboardingResponse reject(UUID requestId, UUID reviewerId, String note) {
        requireActor(reviewerId);
        SiteOnboardingRequest request = loadForUpdate(requestId);
        String reviewNote = note == null ? "" : note.trim();
        if (reviewNote.isEmpty()) {
            throw new BusinessRuleException("Lý do từ chối là bắt buộc");
        }
        if (request.getStatus() != SiteOnboardingStatus.PENDING) {
            throw new BusinessRuleException("Yêu cầu onboarding không còn chờ duyệt");
        }
        request.setStatus(SiteOnboardingStatus.REJECTED);
        request.setReviewedBy(reviewerId);
        request.setReviewNote(reviewNote);
        request.setReviewedAt(Instant.now());
        request.setUpdatedAt(request.getReviewedAt());
        SiteOnboardingRequest saved = siteOnboardingRequestRepository.save(request);
        audit(reviewerId, saved, SiteOnboardingStatus.PENDING, "REJECTED", reviewNote);
        return toResponse(saved);
    }

    private void grantSiteManager(String siteCode, UUID userId, UUID reviewerId) {
        SiteMembership membership = siteMembershipRepository
                .findBySiteCodeIgnoreCaseAndUserId(siteCode, userId)
                .orElseGet(() -> SiteMembership.builder()
                        .siteCode(siteCode)
                        .userId(userId)
                        .build());
        membership.setSiteCode(siteCode);
        membership.setUserId(userId);
        membership.setRole(SiteRole.SITE_MANAGER);
        membership.setActive(true);
        membership.setGrantedBy(reviewerId);
        siteMembershipRepository.save(membership);
    }

    private void audit(
            UUID actorId,
            SiteOnboardingRequest request,
            SiteOnboardingStatus previousStatus,
            String action,
            String reason) {
        adminAuditService.append(new AdminAuditCommand(
                actorId,
                "SITE_ONBOARDING",
                request.getId(),
                action,
                null,
                null,
                reason,
                "site-onboarding:" + action + ":" + request.getId(),
                Map.of(
                        "siteCode", request.getSiteCode(),
                        "requesterId", request.getRequesterId().toString(),
                        "previousStatus", previousStatus.name(),
                        "newStatus", request.getStatus().name())));
    }

    private SiteOnboardingRequest loadForUpdate(UUID requestId) {
        return siteOnboardingRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu onboarding"));
    }

    private SiteOnboardingResponse toResponse(SiteOnboardingRequest request) {
        return new SiteOnboardingResponse(
                request.getId(),
                request.getRequesterId(),
                request.getSiteCode(),
                request.getRepresentativeName(),
                request.getOrganizationName(),
                request.getRepresentativeEmail(),
                request.getEvidenceNote(),
                request.getStatus(),
                request.getReviewedBy(),
                request.getReviewNote(),
                request.getCreatedAt(),
                request.getReviewedAt());
    }

    private static void requireActor(UUID actorId) {
        if (actorId == null) {
            throw new AuthException("Unauthorized");
        }
    }

    private static String required(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessRuleException(message);
        }
        return value.trim();
    }

    private static String normalizeSiteCode(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            throw new BusinessRuleException("Không tìm thấy site workspace");
        }
        return siteCode.trim().toLowerCase();
    }

    private static void requirePlainEvidence(String evidenceNote) {
        if (EMBEDDED_SECRET.matcher(evidenceNote).find()) {
            throw new BusinessRuleException("Ghi chú xác minh chỉ được là văn bản");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
