package com.histar.be.fieldtrip.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.FieldTripReportResponse;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.SiteFieldTripScheduleResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import com.histar.be.fieldtrip.repository.FieldTripReportSnapshotRepository;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.repository.SiteWorkspaceSettingsRepository;
import com.histar.be.siteworkspace.service.SiteWorkspaceAccessService;
import jakarta.persistence.OptimisticLockException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FieldTripLifecycleService {

    private static final String STALE_VERSION = "Chuyến đi đã được cập nhật, vui lòng tải lại";
    private static final Set<SiteRole> SITE_MANAGERS = Set.of(SiteRole.SITE_MANAGER);

    private final FieldTripRepository fieldTripRepository;
    private final FieldTripAccessService fieldTripAccessService;
    private final SiteWorkspaceAccessService siteWorkspaceAccessService;
    private final SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository;
    private final LmsAssignmentService lmsAssignmentService;
    private final StudyGroupRepository studyGroupRepository;
    private final AdminAuditService adminAuditService;
    private final FieldTripReportSnapshotRepository snapshotRepository;
    private final FieldTripReportAssembler reportAssembler;

    public FieldTripLifecycleService(
            FieldTripRepository fieldTripRepository,
            FieldTripAccessService fieldTripAccessService,
            SiteWorkspaceAccessService siteWorkspaceAccessService,
            SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository,
            LmsAssignmentService lmsAssignmentService,
            StudyGroupRepository studyGroupRepository,
            AdminAuditService adminAuditService,
            FieldTripReportSnapshotRepository snapshotRepository,
            FieldTripReportAssembler reportAssembler) {
        this.fieldTripRepository = fieldTripRepository;
        this.fieldTripAccessService = fieldTripAccessService;
        this.siteWorkspaceAccessService = siteWorkspaceAccessService;
        this.siteWorkspaceSettingsRepository = siteWorkspaceSettingsRepository;
        this.lmsAssignmentService = lmsAssignmentService;
        this.studyGroupRepository = studyGroupRepository;
        this.adminAuditService = adminAuditService;
        this.snapshotRepository = snapshotRepository;
        this.reportAssembler = reportAssembler;
    }

    @Transactional
    public FieldTripResponse requestConfirmation(UUID teacherId, UUID tripId, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        requireVersion(trip, expectedVersion);
        if (!requiresConfirmation(trip.getSiteCode())) {
            throw new BusinessRuleException("Di tích không yêu cầu xác nhận chuyến");
        }
        if (trip.getStatus() != FieldTripStatus.DRAFT) {
            throw new BusinessRuleException("Chỉ chuyến nháp mới được gửi xác nhận");
        }
        return change(
                trip,
                teacherId,
                "REQUEST_CONFIRMATION",
                FieldTripStatus.PENDING_SITE,
                SiteConfirmationStatus.PENDING,
                null);
    }

    @Transactional
    public FieldTripResponse confirm(UUID actorId, String siteCode, UUID tripId, long expectedVersion) {
        siteWorkspaceAccessService.requireAny(actorId, siteCode, SITE_MANAGERS);
        FieldTrip trip = load(tripId);
        requireSameSite(trip, siteCode);
        requireVersion(trip, expectedVersion);
        requireWaitingForSite(trip);
        return change(
                trip,
                actorId,
                "CONFIRM",
                FieldTripStatus.CONFIRMED,
                SiteConfirmationStatus.CONFIRMED,
                trip.getSiteNote());
    }

    @Transactional
    public FieldTripResponse decline(UUID actorId, String siteCode, UUID tripId, long expectedVersion, String note) {
        siteWorkspaceAccessService.requireAny(actorId, siteCode, SITE_MANAGERS);
        FieldTrip trip = load(tripId);
        requireSameSite(trip, siteCode);
        requireVersion(trip, expectedVersion);
        requireWaitingForSite(trip);
        return change(trip, actorId, "DECLINE", FieldTripStatus.DRAFT, SiteConfirmationStatus.DECLINED, cleanNote(note));
    }

    @Transactional
    public FieldTripResponse start(UUID teacherId, UUID tripId, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        if (trip.getStatus() == FieldTripStatus.ACTIVE) {
            lmsAssignmentService.ensureForFieldTrip(trip);
            return toResponse(trip);
        }
        requireVersion(trip, expectedVersion);
        if (!canStart(trip)) {
            throw new BusinessRuleException("Chuyến đi chưa thể bắt đầu");
        }
        lmsAssignmentService.ensureForFieldTrip(trip);
        return change(
                trip,
                teacherId,
                "START",
                FieldTripStatus.ACTIVE,
                trip.getSiteConfirmationStatus(),
                trip.getSiteNote());
    }

    @Transactional
    public FieldTripResponse complete(UUID teacherId, UUID tripId, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        requireVersion(trip, expectedVersion);
        if (trip.getStatus() != FieldTripStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ chuyến đang diễn ra mới được kết thúc");
        }
        FieldTripResponse response = change(
                trip,
                teacherId,
                "COMPLETE",
                FieldTripStatus.COMPLETED,
                trip.getSiteConfirmationStatus(),
                trip.getSiteNote());
        storeSnapshotIfAbsent(trip);
        return response;
    }

    @Transactional
    public FieldTripResponse cancel(UUID teacherId, UUID tripId, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        requireVersion(trip, expectedVersion);
        if (trip.getStatus() != FieldTripStatus.DRAFT
                && trip.getStatus() != FieldTripStatus.PENDING_SITE
                && trip.getStatus() != FieldTripStatus.CONFIRMED) {
            throw new BusinessRuleException("Chỉ hủy được chuyến chưa bắt đầu");
        }
        return change(
                trip,
                teacherId,
                "CANCEL",
                FieldTripStatus.CANCELLED,
                trip.getSiteConfirmationStatus(),
                trip.getSiteNote());
    }

    @Transactional(readOnly = true)
    public List<SiteFieldTripScheduleResponse> listForSite(UUID userId, String siteCode) {
        siteWorkspaceAccessService.requireAny(userId, siteCode, SITE_MANAGERS);
        return fieldTripRepository.findBySiteCodeIgnoreCaseOrderByVisitStartAsc(siteCode).stream()
                .map(trip -> new SiteFieldTripScheduleResponse(
                        trip.getId(),
                        trip.getTitle(),
                        trip.getVisitStart(),
                        trip.getVisitEnd(),
                        trip.getStatus(),
                        trip.getSiteConfirmationStatus(),
                        studyGroupRepository.countByFieldTripId(trip.getId()),
                        trip.getRowVersion()))
                .toList();
    }

    private void storeSnapshotIfAbsent(FieldTrip trip) {
        if (snapshotRepository.findByFieldTripId(trip.getId()).isPresent()) {
            return;
        }
        FieldTripReportResponse report = reportAssembler.assemble(trip, Instant.now());
        snapshotRepository.save(reportAssembler.toSnapshot(trip.getId(), report));
    }

    private FieldTripResponse change(
            FieldTrip trip,
            UUID actorId,
            String action,
            FieldTripStatus toStatus,
            SiteConfirmationStatus toConfirmation,
            String siteNote) {
        FieldTripStatus fromStatus = trip.getStatus();
        SiteConfirmationStatus fromConfirmation = trip.getSiteConfirmationStatus();
        long oldVersion = trip.getRowVersion();
        trip.setStatus(toStatus);
        trip.setSiteConfirmationStatus(toConfirmation);
        trip.setSiteNote(siteNote);
        trip.setUpdatedAt(Instant.now());
        FieldTrip saved = save(trip);
        audit(actorId, action, saved, fromStatus, fromConfirmation, oldVersion);
        return toResponse(saved);
    }

    private boolean canStart(FieldTrip trip) {
        boolean required = requiresConfirmation(trip.getSiteCode());
        if (trip.getStatus() == FieldTripStatus.CONFIRMED) {
            return true;
        }
        return trip.getStatus() == FieldTripStatus.DRAFT && !required;
    }

    private boolean requiresConfirmation(String siteCode) {
        return siteWorkspaceSettingsRepository
                .findBySiteCodeIgnoreCase(siteCode)
                .map(settings -> settings.isRequiresTripConfirmation())
                .orElse(false);
    }

    private void requireWaitingForSite(FieldTrip trip) {
        if (trip.getStatus() != FieldTripStatus.PENDING_SITE
                || trip.getSiteConfirmationStatus() != SiteConfirmationStatus.PENDING) {
            throw new BusinessRuleException("Chuyến đi không đang chờ xác nhận");
        }
    }

    private void requireSameSite(FieldTrip trip, String siteCode) {
        String tripSite = trip.getSiteCode() == null ? "" : trip.getSiteCode().trim();
        String requested = siteCode == null ? "" : siteCode.trim();
        if (!tripSite.equalsIgnoreCase(requested)) {
            throw new BusinessRuleException("Không có quyền truy cập site workspace này");
        }
    }

    private void requireVersion(FieldTrip trip, long expectedVersion) {
        if (trip.getRowVersion() == null || trip.getRowVersion() != expectedVersion) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    private FieldTrip load(UUID tripId) {
        return fieldTripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi"));
    }

    private FieldTrip save(FieldTrip trip) {
        try {
            return fieldTripRepository.saveAndFlush(trip);
        } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    private void audit(
            UUID actorId,
            String action,
            FieldTrip saved,
            FieldTripStatus fromStatus,
            SiteConfirmationStatus fromConfirmation,
            long oldVersion) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("tripId", saved.getId().toString());
        metadata.put("siteCode", saved.getSiteCode());
        metadata.put("fromStatus", fromStatus.name());
        metadata.put("toStatus", saved.getStatus().name());
        metadata.put("fromSiteConfirmationStatus", fromConfirmation.name());
        metadata.put("toSiteConfirmationStatus", saved.getSiteConfirmationStatus().name());
        adminAuditService.append(new AdminAuditCommand(
                actorId,
                "FIELD_TRIP",
                saved.getId(),
                action,
                oldVersion,
                saved.getRowVersion(),
                null,
                null,
                metadata));
    }

    private static String cleanNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private FieldTripResponse toResponse(FieldTrip trip) {
        return new FieldTripResponse(
                trip.getId(),
                trip.getOrgId(),
                trip.getSiteCode(),
                trip.getTitle(),
                trip.getVisitStart(),
                trip.getVisitEnd(),
                trip.getQuestId(),
                trip.getCampaignId(),
                trip.getJoinCode(),
                trip.getStatus(),
                trip.getSiteConfirmationStatus(),
                trip.getSiteNote(),
                trip.getCreatedBy(),
                trip.getCreatedAt(),
                trip.getUpdatedAt(),
                trip.getRowVersion());
    }
}
