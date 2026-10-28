package com.histar.be.fieldtrip.service;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
/** Group provisioning reserved for teacher-managed field trips. */
@Service
public class TripGroupService {
    private static final String CODE_ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
 private static final SecureRandom RANDOM=new SecureRandom();
    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final HistarOrgProperties histarOrgProperties;
    private final TripGroupService self;
    public TripGroupService(StudyGroupRepository groups, StudyGroupMemberRepository members,
            HistarOrgProperties properties, @Lazy TripGroupService self) {
        this.studyGroupRepository = groups;
        this.studyGroupMemberRepository = members;
        this.histarOrgProperties = properties;
        this.self = self == null ? this : self;
    }

    @Transactional
    public GroupSummaryResponse joinTripGroup(UUID userId, UUID groupId) {
        StudyGroup group = studyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        if (group.getFieldTripId() == null) {
            throw new BusinessRuleException("Nhóm không thuộc chuyến đi");
        }
        if (studyGroupMemberRepository.findByGroupIdAndUserId(group.getId(), userId).isEmpty()) {
            joinInternal(group, userId);
        }
        int count = studyGroupMemberRepository.findByGroupId(group.getId()).size();
        return toSummary(group, count);
    }

    public GroupSummaryResponse createTripGroup(
            UUID createdBy,
            UUID orgId,
            UUID fieldTripId,
            String name,
            ParticipationMode participationMode,
            Integer declaredSize) {
        String trimmed = requireTripGroupName(name);
        requireDeclaredSize(participationMode, declaredSize);
        if (fieldTripId == null) {
            throw new BusinessRuleException("Nhóm không thuộc chuyến đi");
        }
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                return self.createTripGroupOnce(
                        createdBy, orgId, fieldTripId, trimmed, participationMode, declaredSize);
            } catch (DataIntegrityViolationException ex) {
                // Isolated TX rolled back — retry with a new code in a fresh transaction.
            }
        }
        throw new BusinessRuleException("Không thể tạo mã nhóm, vui lòng thử lại");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GroupSummaryResponse createTripGroupOnce(
            UUID createdBy,
            UUID orgId,
            UUID fieldTripId,
            String name,
            ParticipationMode participationMode,
            Integer declaredSize) {
        String code = generateUniqueCode(6);
        Instant now = Instant.now();
        StudyGroup group = StudyGroup.builder()
                .name(name.trim())
                .code(code)
                .createdBy(createdBy)
                .createdAt(now)
                .expiresAt(now.plus(histarOrgProperties.getGroup().getCodeTtlDays(), ChronoUnit.DAYS))
                .orgId(orgId)
                .teamMode("QUEST")
                .fieldTripId(fieldTripId)
                .participationMode(participationMode)
                .declaredSize(declaredSize)
                .build();
        studyGroupRepository.save(group);
        return toSummary(group, 0);
    }

    private void joinInternal(StudyGroup group, UUID userId) {
        studyGroupMemberRepository.save(StudyGroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .joinedAt(Instant.now())
                .build());
    }

    private GroupSummaryResponse toSummary(StudyGroup group, int memberCount) {
        return new GroupSummaryResponse(
                group.getId(),
                group.getName(),
                group.getCode(),
                group.getExpiresAt(),
                memberCount,
                group.getOrgId(),
                group.getQuestId(),
                group.getFieldTripId(),
                group.getParticipationMode(),
                group.getDeclaredSize());
    }

    private static String requireTripGroupName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() < 2 || trimmed.length() > 255) {
            throw new BusinessRuleException("Tên nhóm không hợp lệ");
        }
        return trimmed;
    }

    private static void requireDeclaredSize(ParticipationMode participationMode, Integer declaredSize) {
        if (participationMode == null) {
            throw new BusinessRuleException("Cần chọn kiểu tham gia");
        }
        if (participationMode == ParticipationMode.SHARED_DEVICE && (declaredSize == null || declaredSize < 1)) {
            throw new BusinessRuleException("Nhóm dùng chung thiết bị cần sĩ số dự kiến từ 1");
        }
        if (declaredSize != null && declaredSize < 1) {
            throw new BusinessRuleException("Cần sĩ số dự kiến từ 1");
        }
    }

    private String generateUniqueCode(int length) {
        for (int attempt = 0; attempt < 30; attempt++) {
            String code = randomCode(length);
            if (studyGroupRepository.findByCodeIgnoreCase(code).isEmpty()) {
                return code;
            }
        }
        throw new BusinessRuleException("Không thể tạo mã nhóm, vui lòng thử lại");
    }

    private static String randomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
