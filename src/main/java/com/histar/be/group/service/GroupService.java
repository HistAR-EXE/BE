package com.histar.be.group.service;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.group.dto.CreateGroupRequest;
import com.histar.be.group.dto.GroupDetailResponse;
import com.histar.be.group.dto.GroupMemberQuestProgressResponse;
import com.histar.be.group.dto.GroupMemberResponse;
import com.histar.be.group.dto.GroupProgressResponse;
import com.histar.be.group.dto.GroupQuestProgressResponse;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.userquestprogress.entity.UserQuestProgress;
import com.histar.be.userquestprogress.repository.UserQuestProgressRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final ProfileRepository profileRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final QuestRepository questRepository;
    private final HistarOrgProperties histarOrgProperties;

    @Transactional
    public GroupSummaryResponse createGroup(UUID userId, CreateGroupRequest request) {
        String code = generateUniqueCode(6);
        Instant now = Instant.now();
        StudyGroup group = StudyGroup.builder()
                .name(request.name().trim())
                .code(code)
                .createdBy(userId)
                .createdAt(now)
                .expiresAt(now.plus(histarOrgProperties.getGroup().getCodeTtlDays(), ChronoUnit.DAYS))
                .build();
        studyGroupRepository.save(group);
        joinInternal(group, userId);
        return toSummary(group, 1);
    }

    @Transactional
    public GroupSummaryResponse joinGroup(UUID userId, String codeRaw) {
        String code = codeRaw == null ? "" : codeRaw.trim().toUpperCase();
        if (code.length() != 6) {
            throw new BusinessRuleException("Mã nhóm phải có 6 ký tự");
        }
        StudyGroup group = studyGroupRepository
                .findByCodeIgnoreCase(code)
                .orElseThrow(() -> new BusinessRuleException("Mã nhóm không hợp lệ"));
        if (group.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Mã nhóm đã hết hạn");
        }
        if (studyGroupMemberRepository.findByGroupIdAndUserId(group.getId(), userId).isEmpty()) {
            long userGroupCount = studyGroupMemberRepository.countByUserId(userId);
            if (userGroupCount >= histarOrgProperties.getGroup().getMaxGroupsPerUser()) {
                throw new BusinessRuleException(
                        "Bạn đã tham gia tối đa " + histarOrgProperties.getGroup().getMaxGroupsPerUser() + " nhóm");
            }
            joinInternal(group, userId);
        }
        long count = studyGroupMemberRepository.findByGroupId(group.getId()).size();
        return toSummary(group, (int) count);
    }

    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> listMine(UUID userId) {
        return studyGroupMemberRepository.findByUserId(userId).stream()
                .map(member -> {
                    StudyGroup group = studyGroupRepository
                            .findById(member.getGroupId())
                            .orElse(null);
                    if (group == null) {
                        return null;
                    }
                    int count = studyGroupMemberRepository.findByGroupId(group.getId()).size();
                    return toSummary(group, count);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupDetailResponse getGroup(UUID userId, UUID groupId) {
        requireMember(userId, groupId);
        StudyGroup group = studyGroupRepository
                .findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        List<GroupMemberResponse> members = buildMemberResponses(studyGroupMemberRepository.findByGroupId(groupId));
        return new GroupDetailResponse(group.getId(), group.getName(), group.getCode(), members);
    }

    @Transactional(readOnly = true)
    public GroupProgressResponse getProgress(UUID userId, UUID groupId) {
        requireMember(userId, groupId);
        List<StudyGroupMember> members = studyGroupMemberRepository.findByGroupId(groupId);
        List<UUID> userIds = members.stream().map(StudyGroupMember::getUserId).toList();
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(userIds).forEach(p -> profiles.put(p.getId(), p));

        List<UserQuestProgress> allProgress = userQuestProgressRepository.findAll().stream()
                .filter(p -> userIds.contains(p.getUserId()))
                .toList();
        Set<UUID> questIds = new HashSet<>();
        Map<UUID, List<UserQuestProgress>> byQuest = new HashMap<>();
        for (UserQuestProgress progress : allProgress) {
            questIds.add(progress.getQuestId());
            byQuest.computeIfAbsent(progress.getQuestId(), k -> new ArrayList<>()).add(progress);
        }

        List<GroupQuestProgressResponse> quests = new ArrayList<>();
        for (UUID questId : questIds) {
            Quest quest = questRepository.findById(questId).orElse(null);
            String title = quest != null ? quest.getTitle() : "Quest";
            List<GroupMemberQuestProgressResponse> memberRows = new ArrayList<>();
            for (StudyGroupMember member : members) {
                Profile profile = profiles.get(member.getUserId());
                UserQuestProgress progress = byQuest.getOrDefault(questId, List.of()).stream()
                        .filter(p -> p.getUserId().equals(member.getUserId()))
                        .findFirst()
                        .orElse(null);
                memberRows.add(new GroupMemberQuestProgressResponse(
                        member.getUserId(),
                        profile != null ? profile.getDisplayName() : "Member",
                        progress != null ? progress.getStatus() : "not_started",
                        progress != null ? progress.getCurrentStep() : 0,
                        progress != null ? progress.getStepsTotal() : 0,
                        computePercent(progress)));
            }
            quests.add(new GroupQuestProgressResponse(questId, title, memberRows));
        }
        return new GroupProgressResponse(quests);
    }

    private void joinInternal(StudyGroup group, UUID userId) {
        studyGroupMemberRepository.save(StudyGroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .joinedAt(Instant.now())
                .build());
    }

    private void requireMember(UUID userId, UUID groupId) {
        if (studyGroupMemberRepository.findByGroupIdAndUserId(groupId, userId).isEmpty()) {
            throw new AuthException("Bạn không phải thành viên nhóm này");
        }
    }

    private GroupSummaryResponse toSummary(StudyGroup group, int memberCount) {
        return new GroupSummaryResponse(group.getId(), group.getName(), group.getCode(), group.getExpiresAt(), memberCount);
    }

    private List<GroupMemberResponse> buildMemberResponses(List<StudyGroupMember> members) {
        Map<UUID, Profile> profiles = new HashMap<>();
        List<UUID> ids = members.stream().map(StudyGroupMember::getUserId).toList();
        profileRepository.findAllById(ids).forEach(p -> profiles.put(p.getId(), p));
        return members.stream()
                .map(m -> {
                    Profile p = profiles.get(m.getUserId());
                    return new GroupMemberResponse(
                            m.getUserId(),
                            p != null ? p.getDisplayName() : "Member",
                            p != null ? p.getAvatarUrl() : null);
                })
                .toList();
    }

    private static int computePercent(UserQuestProgress progress) {
        if (progress == null || progress.getStepsTotal() == null || progress.getStepsTotal() <= 0) {
            return progress != null && "completed".equalsIgnoreCase(progress.getStatus()) ? 100 : 0;
        }
        int current = progress.getCurrentStep() == null ? 0 : progress.getCurrentStep();
        return Math.min(100, (int) Math.round(current * 100.0 / progress.getStepsTotal()));
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
