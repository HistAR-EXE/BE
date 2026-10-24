package com.histar.be.fieldtrip.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.CreateFieldTripRequest;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.UpdateFieldTripRequest;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.campaign.QuestCampaign;
import com.histar.be.quest.campaign.QuestCampaignAdminService;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import jakarta.persistence.OptimisticLockException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FieldTripService {

    private static final String JOIN_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int JOIN_CODE_LENGTH = 8;
    private static final int MAX_JOIN_CODE_ATTEMPTS = 8;
    private static final String STALE_VERSION = "Chuyến đi đã được cập nhật, vui lòng tải lại";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final FieldTripRepository fieldTripRepository;
    private final FieldTripAccessService fieldTripAccessService;
    private final LocationRepository locationRepository;
    private final QuestRepository questRepository;
    private final QuestCampaignRepository campaignRepository;
    private final QuestCampaignAdminService campaignAdminService;
    private FieldTripService self = this;

    public FieldTripService(
            FieldTripRepository fieldTripRepository,
            FieldTripAccessService fieldTripAccessService,
            LocationRepository locationRepository,
            QuestRepository questRepository,
            QuestCampaignRepository campaignRepository,
            QuestCampaignAdminService campaignAdminService) {
        this.fieldTripRepository = fieldTripRepository;
        this.fieldTripAccessService = fieldTripAccessService;
        this.locationRepository = locationRepository;
        this.questRepository = questRepository;
        this.campaignRepository = campaignRepository;
        this.campaignAdminService = campaignAdminService;
    }

    @Autowired
    void setSelf(@Lazy FieldTripService self) {
        this.self = self;
    }

    @Transactional
    public FieldTripResponse create(UUID teacherId, CreateFieldTripRequest request) {
        fieldTripAccessService.requirePlanningTeacher(teacherId, request.orgId());
        String title = requireTitle(request.title());
        requireVisitWindow(request.visitStart(), request.visitEnd());
        Location location = requireLocation(request.siteCode());
        Quest quest = requireQuestAtLocation(request.questId(), location);
        UUID campaignId = requireCampaign(request.campaignId(), quest.getId());
        Instant now = Instant.now();
        for (int attempt = 0; attempt < MAX_JOIN_CODE_ATTEMPTS; attempt++) {
            String joinCode = randomJoinCode();
            if (fieldTripRepository.existsByJoinCodeIgnoreCase(joinCode)) {
                continue;
            }
            FieldTrip trip = FieldTrip.builder()
                    .orgId(request.orgId())
                    .siteCode(canonicalSiteCode(location))
                    .title(title)
                    .visitStart(request.visitStart())
                    .visitEnd(request.visitEnd())
                    .questId(quest.getId())
                    .campaignId(campaignId)
                    .joinCode(joinCode)
                    .status(FieldTripStatus.DRAFT)
                    .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                    .createdBy(teacherId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            try {
                return toResponse(self.persistDraft(trip));
            } catch (DataIntegrityViolationException ex) {
                if (!isJoinCodeConflict(ex)) {
                    throw ex;
                }
            }
        }
        throw new BusinessRuleException("Không thể tạo mã chuyến, vui lòng thử lại");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FieldTrip persistDraft(FieldTrip trip) {
        return fieldTripRepository.saveAndFlush(trip);
    }

    @Transactional
    public FieldTripResponse update(
            UUID teacherId, UUID tripId, UpdateFieldTripRequest request, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        requireCurrentVersion(trip, expectedVersion);
        String title = requireTitle(request.title());
        requireVisitWindow(request.visitStart(), request.visitEnd());
        Location location = requireLocation(request.siteCode());
        Quest quest = requireQuestAtLocation(request.questId(), location);
        trip.setTitle(title);
        trip.setSiteCode(canonicalSiteCode(location));
        trip.setVisitStart(request.visitStart());
        trip.setVisitEnd(request.visitEnd());
        trip.setQuestId(quest.getId());
        trip.setCampaignId(requireCampaign(request.campaignId(), quest.getId()));
        trip.setUpdatedAt(Instant.now());
        try {
            return toResponse(fieldTripRepository.saveAndFlush(trip));
        } catch (OptimisticLockingFailureException | OptimisticLockException ex) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    @Transactional(readOnly = true)
    public List<FieldTripResponse> listMine(UUID teacherId) {
        fieldTripAccessService.requireActiveMember(teacherId);
        return fieldTripRepository.findByCreatedByOrderByCreatedAtDesc(teacherId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FieldTripResponse get(UUID teacherId, UUID tripId) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanView(teacherId, trip);
        return toResponse(trip);
    }

    private FieldTrip load(UUID tripId) {
        return fieldTripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi"));
    }

    private void requireCurrentVersion(FieldTrip trip, long expectedVersion) {
        if (trip.getRowVersion() == null || trip.getRowVersion() != expectedVersion) {
            throw new NarrativeConflictException(ErrorCode.VERSION_CONFLICT, STALE_VERSION);
        }
    }

    private Location requireLocation(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            throw new BusinessRuleException("Không tìm thấy di tích");
        }
        String normalized = siteCode.trim().toLowerCase(Locale.ROOT);
        return locationRepository
                .findBySiteCodeIgnoreCase(normalized)
                .orElseThrow(() -> new BusinessRuleException("Không tìm thấy di tích"));
    }

    private String canonicalSiteCode(Location location) {
        String siteCode = location.getSiteCode() == null
                ? ""
                : location.getSiteCode().trim().toLowerCase(Locale.ROOT);
        if (!siteCode.matches("^[a-z0-9][a-z0-9-]{0,63}$")) {
            throw new BusinessRuleException("Không tìm thấy di tích");
        }
        return siteCode;
    }

    private Quest requireQuestAtLocation(UUID questId, Location location) {
        Quest quest = questRepository
                .findById(questId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest not found"));
        if (quest.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Quest not found");
        }
        if (location.getId() == null || !location.getId().equals(quest.getLocationId())) {
            throw new BusinessRuleException("Quest không thuộc di tích đã chọn");
        }
        return quest;
    }

    private UUID requireCampaign(UUID campaignId, UUID questId) {
        if (campaignId == null) {
            throw new BusinessRuleException("Campaign không khả dụng cho quest này");
        }
        QuestCampaign campaign = campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Quest campaign not found"));
        if (!questId.equals(campaign.getQuestId())) {
            throw new BusinessRuleException("Campaign không khả dụng cho quest này");
        }
        campaignAdminService.requireAvailableNow(campaign, Instant.now());
        return campaign.getId();
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank() || title.trim().length() > 255) {
            throw new BusinessRuleException("Tiêu đề chuyến đi không hợp lệ");
        }
        return title.trim();
    }

    private void requireVisitWindow(Instant visitStart, Instant visitEnd) {
        if (visitStart == null || visitEnd == null || !visitEnd.isAfter(visitStart)) {
            throw new BusinessRuleException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
    }

    private boolean isJoinCodeConflict(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("join_code")) {
                return true;
            }
            Throwable next = current.getCause();
            current = next == current ? null : next;
        }
        return false;
    }

    private static String randomJoinCode() {
        StringBuilder code = new StringBuilder(JOIN_CODE_LENGTH);
        for (int i = 0; i < JOIN_CODE_LENGTH; i++) {
            code.append(JOIN_CODE_ALPHABET.charAt(RANDOM.nextInt(JOIN_CODE_ALPHABET.length())));
        }
        return code.toString();
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
