package com.histar.be.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.billing.service.MultiplayerAccessService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.repository.QuestRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private StudyGroupMemberRepository studyGroupMemberRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserQuestProgressRepository userQuestProgressRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private MultiplayerAccessService multiplayerAccessService;

    private GroupService groupService;

    @BeforeEach
    void setUp() {
        HistarOrgProperties orgProperties = new HistarOrgProperties();
        orgProperties.getGroup().setMaxGroupsPerUser(20);
        groupService = new GroupService(
                studyGroupRepository,
                studyGroupMemberRepository,
                profileRepository,
                userQuestProgressRepository,
                questRepository,
                orgProperties,
                multiplayerAccessService,
                null);
    }

    @Test
    void joinGroup_rejectsExpiredCode() {
        UUID userId = UUID.randomUUID();
        StudyGroup group = StudyGroup.builder()
                .id(UUID.randomUUID())
                .name("Expired Room")
                .code("ABC123")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now().minusSeconds(86_400))
                .expiresAt(Instant.now().minusSeconds(60))
                .build();

        when(studyGroupRepository.findByCodeIgnoreCase("ABC123")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.joinGroup(userId, "ABC123"))
                .isInstanceOf(BusinessRuleException.class)
                .satisfies(ex -> {
                    BusinessRuleException bre = (BusinessRuleException) ex;
                    assertThat(bre.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_RULE);
                    assertThat(bre.getMessage()).contains("hết hạn");
                });
    }

    @Test
    void joinGroup_rejectsInvalidLength() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> groupService.joinGroup(userId, "AB"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("6 ký tự");
    }

    @Test
    void joinTripGroup_addsMemberWhenGroupBelongsToATrip() {
        UUID userId = UUID.randomUUID();
        UUID tripId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        StudyGroup group = tripGroup(groupId, tripId, ParticipationMode.SHARED_DEVICE, 8);
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupMemberRepository.findByGroupIdAndUserId(groupId, userId)).thenReturn(Optional.empty());
        when(studyGroupMemberRepository.save(any(StudyGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studyGroupMemberRepository.findByGroupId(groupId))
                .thenReturn(List.of(member(groupId, userId)));

        GroupSummaryResponse summary = groupService.joinTripGroup(userId, groupId);

        ArgumentCaptor<StudyGroupMember> saved = ArgumentCaptor.forClass(StudyGroupMember.class);
        verify(studyGroupMemberRepository).save(saved.capture());
        assertThat(saved.getValue().getGroupId()).isEqualTo(groupId);
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(summary.fieldTripId()).isEqualTo(tripId);
        assertThat(summary.participationMode()).isEqualTo(ParticipationMode.SHARED_DEVICE);
        assertThat(summary.declaredSize()).isEqualTo(8);
        assertThat(summary.memberCount()).isEqualTo(1);
        verify(profileRepository, never()).save(any());
        verify(multiplayerAccessService, never()).assertMultiplayerAccess(any());
    }

    @Test
    void joinTripGroup_doesNotWriteAgainWhenAlreadyAMember() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        StudyGroup group = tripGroup(groupId, UUID.randomUUID(), ParticipationMode.INDIVIDUAL, null);
        StudyGroupMember existing = member(groupId, userId);
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));
        when(studyGroupMemberRepository.findByGroupIdAndUserId(groupId, userId))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(studyGroupMemberRepository.save(any(StudyGroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(studyGroupMemberRepository.findByGroupId(groupId)).thenReturn(List.of(existing));

        groupService.joinTripGroup(userId, groupId);
        GroupSummaryResponse second = groupService.joinTripGroup(userId, groupId);

        verify(studyGroupMemberRepository, times(1)).save(any(StudyGroupMember.class));
        assertThat(second.memberCount()).isEqualTo(1);
        assertThat(second.declaredSize()).isNull();
    }

    @Test
    void joinTripGroup_rejectsGroupThatIsNotOnATrip() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        StudyGroup group = StudyGroup.builder()
                .id(groupId)
                .name("Phòng thường")
                .code("ABC123")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3_600))
                .build();
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.joinTripGroup(userId, groupId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chuyến đi");
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void joinTripGroup_rejectsUnknownGroup() {
        UUID userId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.joinTripGroup(userId, groupId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void joinGroup_rejectsTripLinkedGroupWithoutMembership() {
        UUID userId = UUID.randomUUID();
        StudyGroup group = tripGroup(UUID.randomUUID(), UUID.randomUUID(), ParticipationMode.INDIVIDUAL, null);
        when(studyGroupRepository.findByCodeIgnoreCase("ABC123")).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.joinGroup(userId, "ABC123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mã chuyến");
        verify(studyGroupMemberRepository, never()).save(any());
    }

    private static StudyGroup tripGroup(
            UUID groupId, UUID tripId, ParticipationMode mode, Integer declaredSize) {
        return StudyGroup.builder()
                .id(groupId)
                .name("Nhóm chuyến")
                .code("ABC123")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3_600))
                .teamMode("QUEST")
                .fieldTripId(tripId)
                .participationMode(mode)
                .declaredSize(declaredSize)
                .build();
    }

    private static StudyGroupMember member(UUID groupId, UUID userId) {
        return StudyGroupMember.builder()
                .groupId(groupId)
                .userId(userId)
                .joinedAt(Instant.now())
                .build();
    }
}
