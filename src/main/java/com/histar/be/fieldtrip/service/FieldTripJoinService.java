package com.histar.be.fieldtrip.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.CreateTripGroupRequest;
import com.histar.be.fieldtrip.dto.JoinFieldTripRequest;
import com.histar.be.fieldtrip.dto.TripJoinPreviewResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.fieldtrip.service.TripGroupService;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FieldTripJoinService {

    private final FieldTripRepository fieldTripRepository;
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final TripGroupService groupService;
    private final FieldTripAccessService fieldTripAccessService;

    @Transactional
    public GroupSummaryResponse createGroup(UUID teacherId, UUID tripId, CreateTripGroupRequest request) {
        FieldTrip trip = fieldTripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chuyến đi"));
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        if (trip.getStatus() == FieldTripStatus.CANCELLED || trip.getStatus() == FieldTripStatus.COMPLETED) {
            throw new BusinessRuleException("Không thể thêm nhóm vào chuyến đã kết thúc hoặc bị hủy");
        }
        return groupService.createTripGroup(
                teacherId,
                trip.getOrgId(),
                trip.getId(),
                request.name(),
                request.participationMode(),
                request.declaredSize());
    }

    @Transactional(readOnly = true)
    public TripJoinPreviewResponse preview(String code) {
        FieldTrip trip = requireTripByCode(code);
        return new TripJoinPreviewResponse(
                trip.getTitle(),
                trip.getSiteCode(),
                trip.getVisitStart(),
                trip.getVisitEnd(),
                trip.getStatus(),
                studyGroupRepository.findByFieldTripId(trip.getId()).stream()
                        .map(group -> new TripJoinPreviewResponse.GroupPreview(
                                group.getId(),
                                group.getName(),
                                group.getParticipationMode(),
                                group.getDeclaredSize(),
                                (int) studyGroupMemberRepository.countByGroupId(group.getId())))
                        .toList());
    }

    @Transactional
    public GroupSummaryResponse join(UUID userId, JoinFieldTripRequest request) {
        FieldTrip trip = requireTripByCode(request.code());
        requireJoinable(trip);
        StudyGroup group = studyGroupRepository
                .findById(request.groupId())
                .orElseThrow(() -> new BusinessRuleException("Nhóm không thuộc chuyến đi này"));
        if (group.getFieldTripId() == null || !trip.getId().equals(group.getFieldTripId())) {
            throw new BusinessRuleException("Nhóm không thuộc chuyến đi này");
        }
        return groupService.joinTripGroup(userId, group.getId());
    }

    private FieldTrip requireTripByCode(String codeRaw) {
        String code = codeRaw == null ? "" : codeRaw.trim().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) {
            throw new BusinessRuleException("Mã chuyến không hợp lệ");
        }
        return fieldTripRepository
                .findByJoinCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("Mã chuyến không hợp lệ"));
    }

    private static void requireJoinable(FieldTrip trip) {
        if (trip.getStatus() == FieldTripStatus.CANCELLED) {
            throw new BusinessRuleException("Chuyến đi đã bị hủy");
        }
        if (trip.getStatus() == FieldTripStatus.COMPLETED) {
            throw new BusinessRuleException("Chuyến đi đã kết thúc");
        }
        if (trip.getVisitEnd() != null && trip.getVisitEnd().isBefore(Instant.now())) {
            throw new BusinessRuleException("Chuyến đi đã hết hạn");
        }
    }
}
