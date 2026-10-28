package com.histar.be.fieldtrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarOrgProperties;
import com.histar.be.fieldtrip.dto.CreateTripGroupRequest;
import com.histar.be.fieldtrip.dto.JoinFieldTripRequest;
import com.histar.be.fieldtrip.dto.TripJoinPreviewResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.fieldtrip.service.TripGroupService;
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FieldTripJoinServiceTest {

    private static final Instant VISIT_START = Instant.parse("2026-11-01T01:00:00Z");
    private static final String JOIN_CODE = "ABCD2345";

    @Mock
    private FieldTripRepository fieldTripRepository;

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
    private LmsAssignmentService lmsAssignmentService;

    @Mock
    private OrganizationRepository organizationRepository;

    private FieldTripJoinService fieldTripJoinService;

    private UUID teacherId;
    private UUID orgId;
    private UUID studentId;

    @BeforeEach
    void setUp() {
        HistarOrgProperties orgProperties = new HistarOrgProperties();
        orgProperties.getGroup().setMaxGroupsPerUser(20);
        orgProperties.getGroup().setCodeTtlDays(30);
        TripGroupService groupService = new TripGroupService(
                studyGroupRepository,
                studyGroupMemberRepository,
orgProperties,
null);
        fieldTripJoinService = new FieldTripJoinService(
                fieldTripRepository,
                studyGroupRepository,
                studyGroupMemberRepository,
                groupService,
                new FieldTripAccessService(lmsAssignmentService, organizationRepository));
        teacherId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        studentId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preview_listsScheduleAndGroupsWithoutStudentIdentity() {
        FieldTrip trip = trip(FieldTripStatus.CONFIRMED, Instant.now().plus(2, ChronoUnit.DAYS));
        UUID sharedId = UUID.randomUUID();
        UUID individualId = UUID.randomUUID();
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));
        when(studyGroupRepository.findByFieldTripId(trip.getId()))
                .thenReturn(List.of(
                        group(sharedId, trip.getId(), "Nhóm máy", ParticipationMode.SHARED_DEVICE, 12),
                        group(individualId, trip.getId(), "Nhóm riêng", ParticipationMode.INDIVIDUAL, null)));
        when(studyGroupMemberRepository.countByGroupId(sharedId)).thenReturn(2L);
        when(studyGroupMemberRepository.countByGroupId(individualId)).thenReturn(0L);

        TripJoinPreviewResponse preview = fieldTripJoinService.preview("  abcd2345  ");

        assertThat(preview.title()).isEqualTo("Chuyến lớp 6");
        assertThat(preview.siteCode()).isEqualTo("cu-chi");
        assertThat(preview.visitStart()).isEqualTo(VISIT_START);
        assertThat(preview.visitEnd()).isEqualTo(trip.getVisitEnd());
        assertThat(preview.status()).isEqualTo(FieldTripStatus.CONFIRMED);
        assertThat(preview.groups())
                .containsExactly(
                        new TripJoinPreviewResponse.GroupPreview(
                                sharedId, "Nhóm máy", ParticipationMode.SHARED_DEVICE, 12, 2),
                        new TripJoinPreviewResponse.GroupPreview(
                                individualId, "Nhóm riêng", ParticipationMode.INDIVIDUAL, null, 0));
        verify(studyGroupMemberRepository, never()).findByGroupId(any());
        verify(profileRepository, never()).findById(any());
        verify(profileRepository, never()).findAllById(any());
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void preview_returnsClosedStatusWithoutAddingMembership() {
        FieldTrip trip = trip(FieldTripStatus.CANCELLED, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));
        when(studyGroupRepository.findByFieldTripId(trip.getId())).thenReturn(List.of());

        TripJoinPreviewResponse preview = fieldTripJoinService.preview(JOIN_CODE);

        assertThat(preview.status()).isEqualTo(FieldTripStatus.CANCELLED);
        assertThat(preview.groups()).isEmpty();
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void preview_rejectsUnknownCode() {
        when(fieldTripRepository.findByJoinCodeIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTripJoinService.preview("NO-SUCH"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Mã chuyến");
    }

    @Test
    void createGroup_storesQuestTeamOnTheTripWithoutMembership() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();
        stubGroupInsert();

        GroupSummaryResponse response = fieldTripJoinService.createGroup(
                teacherId, trip.getId(), new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null));

        StudyGroup group = savedGroup();
        assertThat(group.getFieldTripId()).isEqualTo(trip.getId());
        assertThat(group.getOrgId()).isEqualTo(orgId);
        assertThat(group.getCreatedBy()).isEqualTo(teacherId);
        assertThat(group.getTeamMode()).isEqualTo("QUEST");
        assertThat(group.getQuestId()).isNull();
        assertThat(group.getParticipationMode()).isEqualTo(ParticipationMode.INDIVIDUAL);
        assertThat(group.getDeclaredSize()).isNull();
        assertThat(group.getName()).isEqualTo("Nhóm A");
        assertThat(group.getCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");
        assertThat(group.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
        assertThat(response.memberCount()).isZero();
        assertThat(response.fieldTripId()).isEqualTo(trip.getId());
        assertThat(response.participationMode()).isEqualTo(ParticipationMode.INDIVIDUAL);
        assertThat(response.declaredSize()).isNull();
        assertThat(response.code()).isEqualTo(group.getCode());
        verify(studyGroupMemberRepository, never()).save(any());
        verify(profileRepository, never()).findById(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void createGroup_storesDeclaredHeadcountForSharedDevice() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();
        stubGroupInsert();

        GroupSummaryResponse response = fieldTripJoinService.createGroup(
                teacherId,
                trip.getId(),
                new CreateTripGroupRequest("Máy chung", ParticipationMode.SHARED_DEVICE, 12));

        StudyGroup group = savedGroup();
        assertThat(group.getParticipationMode()).isEqualTo(ParticipationMode.SHARED_DEVICE);
        assertThat(group.getDeclaredSize()).isEqualTo(12);
        assertThat(response.declaredSize()).isEqualTo(12);
        assertThat(response.memberCount()).isZero();
        verify(studyGroupMemberRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("invalidSharedDeviceSizes")
    void createGroup_rejectsSharedDeviceWithoutPositiveDeclaredSize(Integer declaredSize) {
        FieldTrip trip = trip(FieldTripStatus.ACTIVE, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        teacherId,
                        trip.getId(),
                        new CreateTripGroupRequest("Máy chung", ParticipationMode.SHARED_DEVICE, declaredSize)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sĩ số");
        verify(studyGroupRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void createGroup_rejectsNonPositiveDeclaredSizeOnAnIndividualGroup() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        teacherId, trip.getId(), new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sĩ số");
        verify(studyGroupRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"PENDING_SITE", "CONFIRMED", "ACTIVE"})
    void createGroup_allowsOpenTrip(FieldTripStatus status) {
        FieldTrip trip = trip(status, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();
        stubGroupInsert();

        fieldTripJoinService.createGroup(
                teacherId, trip.getId(), new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null));

        assertThat(savedGroup().getFieldTripId()).isEqualTo(trip.getId());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"CANCELLED", "COMPLETED"})
    void createGroup_rejectsClosedTrip(FieldTripStatus status) {
        FieldTrip trip = trip(status, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        teacherId,
                        trip.getId(),
                        new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể thêm nhóm");
        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createGroup_rejectsClosedTripEvenForPlatformAdmin() {
        FieldTrip trip = trip(FieldTripStatus.COMPLETED, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        UUID.randomUUID(),
                        trip.getId(),
                        new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không thể thêm nhóm");
        verify(studyGroupRepository, never()).save(any());
        verify(lmsAssignmentService, never()).requirePremiumTeacher(any());
    }

    @Test
    void createGroup_rejectsTeacherWhoDoesNotOwnTheTrip() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        UUID otherTeacher = UUID.randomUUID();
        when(lmsAssignmentService.requirePremiumTeacher(otherTeacher))
                .thenReturn(Profile.builder().id(otherTeacher).orgId(orgId).build());

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        otherTeacher,
                        trip.getId(),
                        new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("người tạo");
        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void createGroup_allowsPlatformAdminWhoDidNotCreateTheTrip() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubGroupInsert();
        UUID adminId = UUID.randomUUID();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        fieldTripJoinService.createGroup(
                adminId, trip.getId(), new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null));

        StudyGroup group = savedGroup();
        assertThat(group.getCreatedBy()).isEqualTo(adminId);
        assertThat(group.getFieldTripId()).isEqualTo(trip.getId());
        verify(lmsAssignmentService, never()).requirePremiumTeacher(any());
    }

    @Test
    void createGroup_retriesWhenGeneratedCodeCollides() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubOwner();
        when(studyGroupRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(studyGroupRepository.save(any(StudyGroup.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate code"))
                .thenAnswer(invocation -> {
                    StudyGroup group = invocation.getArgument(0);
                    group.setId(UUID.randomUUID());
                    return group;
                });

        GroupSummaryResponse response = fieldTripJoinService.createGroup(
                teacherId, trip.getId(), new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null));

        assertThat(response.code()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}");
        verify(studyGroupRepository, times(2)).save(any(StudyGroup.class));
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void createGroup_rejectsMissingTrip() {
        UUID tripId = UUID.randomUUID();
        when(fieldTripRepository.findById(tripId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTripJoinService.createGroup(
                        teacherId, tripId, new CreateTripGroupRequest("Nhóm A", ParticipationMode.INDIVIDUAL, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(studyGroupRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"DRAFT", "PENDING_SITE", "CONFIRMED", "ACTIVE"})
    void join_addsCurrentUserToTheSelectedGroup(FieldTripStatus status) {
        FieldTrip trip = trip(status, Instant.now().plus(2, ChronoUnit.DAYS));
        StudyGroup group = group(UUID.randomUUID(), trip.getId(), "Nhóm A", ParticipationMode.INDIVIDUAL, null);
        stubJoinLookup(trip, group);
        when(studyGroupMemberRepository.findByGroupIdAndUserId(group.getId(), studentId)).thenReturn(Optional.empty());
        when(studyGroupMemberRepository.save(any(StudyGroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studyGroupMemberRepository.findByGroupId(group.getId()))
                .thenReturn(List.of(member(group.getId(), studentId)));

        GroupSummaryResponse response = fieldTripJoinService.join(
                studentId, new JoinFieldTripRequest("abcd2345", group.getId()));

        StudyGroupMember saved = savedMember();
        assertThat(saved.getGroupId()).isEqualTo(group.getId());
        assertThat(saved.getUserId()).isEqualTo(studentId);
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.fieldTripId()).isEqualTo(trip.getId());
        verify(studyGroupRepository, never()).save(any());
        verify(profileRepository, never()).save(any());
    }

    @Test
    void join_sharedDeviceKeepsDeclaredSizeAndDoesNotInventStudents() {
        FieldTrip trip = trip(FieldTripStatus.ACTIVE, Instant.now().plus(1, ChronoUnit.HOURS));
        StudyGroup group = group(UUID.randomUUID(), trip.getId(), "Máy chung", ParticipationMode.SHARED_DEVICE, 12);
        stubJoinLookup(trip, group);
        when(studyGroupMemberRepository.findByGroupIdAndUserId(group.getId(), studentId)).thenReturn(Optional.empty());
        when(studyGroupMemberRepository.save(any(StudyGroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studyGroupMemberRepository.findByGroupId(group.getId()))
                .thenReturn(List.of(member(group.getId(), studentId)));

        GroupSummaryResponse response = fieldTripJoinService.join(
                studentId, new JoinFieldTripRequest(JOIN_CODE, group.getId()));

        assertThat(savedMember().getUserId()).isEqualTo(studentId);
        assertThat(response.declaredSize()).isEqualTo(12);
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.participationMode()).isEqualTo(ParticipationMode.SHARED_DEVICE);
        verify(studyGroupMemberRepository, times(1)).save(any());
        verify(profileRepository, never()).save(any());
        verify(studyGroupRepository, never()).save(any());
    }

    @Test
    void join_isIdempotentWhenAlreadyAMember() {
        FieldTrip trip = trip(FieldTripStatus.ACTIVE, Instant.now().plus(2, ChronoUnit.DAYS));
        StudyGroup group = group(UUID.randomUUID(), trip.getId(), "Nhóm A", ParticipationMode.INDIVIDUAL, null);
        StudyGroupMember existing = member(group.getId(), studentId);
        stubJoinLookup(trip, group);
        when(studyGroupMemberRepository.findByGroupIdAndUserId(group.getId(), studentId))
                .thenReturn(Optional.empty(), Optional.of(existing));
        when(studyGroupMemberRepository.save(any(StudyGroupMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(studyGroupMemberRepository.findByGroupId(group.getId())).thenReturn(List.of(existing));

        fieldTripJoinService.join(studentId, new JoinFieldTripRequest(JOIN_CODE, group.getId()));
        GroupSummaryResponse second =
                fieldTripJoinService.join(studentId, new JoinFieldTripRequest(JOIN_CODE, group.getId()));

        verify(studyGroupMemberRepository, times(1)).save(any());
        assertThat(second.memberCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"CANCELLED", "COMPLETED"})
    void join_rejectsClosedTripWithoutMembership(FieldTripStatus status) {
        FieldTrip trip = trip(status, Instant.now().plus(2, ChronoUnit.DAYS));
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));

        String snippet = status == FieldTripStatus.CANCELLED ? "hủy" : "kết thúc";
        assertThatThrownBy(() -> fieldTripJoinService.join(
                        studentId, new JoinFieldTripRequest(JOIN_CODE, UUID.randomUUID())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(snippet);
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void join_rejectsPastVisitEndWithoutMembership() {
        FieldTrip trip = trip(FieldTripStatus.ACTIVE, Instant.now().minusSeconds(30));
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> fieldTripJoinService.join(
                        studentId, new JoinFieldTripRequest(JOIN_CODE, UUID.randomUUID())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn");
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void join_rejectsGroupFromAnotherTripWithoutMembership() {
        FieldTrip trip = trip(FieldTripStatus.CONFIRMED, Instant.now().plus(2, ChronoUnit.DAYS));
        UUID otherTripId = UUID.randomUUID();
        StudyGroup group = group(UUID.randomUUID(), otherTripId, "Nhóm khác", ParticipationMode.INDIVIDUAL, null);
        stubJoinLookup(trip, group);

        assertThatThrownBy(() -> fieldTripJoinService.join(
                        studentId, new JoinFieldTripRequest(JOIN_CODE, group.getId())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc");
        verify(studyGroupMemberRepository, never()).save(any());
    }

    @Test
    void join_rejectsUnknownGroupWithoutMembership() {
        FieldTrip trip = trip(FieldTripStatus.DRAFT, Instant.now().plus(2, ChronoUnit.DAYS));
        UUID groupId = UUID.randomUUID();
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));
        when(studyGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTripJoinService.join(
                        studentId, new JoinFieldTripRequest(JOIN_CODE, groupId)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc");
        verify(studyGroupMemberRepository, never()).save(any());
    }

    static Stream<Integer> invalidSharedDeviceSizes() {
        return Stream.of(null, 0, -2);
    }

    private void stubOwner() {
        when(lmsAssignmentService.requirePremiumTeacher(teacherId))
                .thenReturn(Profile.builder().id(teacherId).orgId(orgId).build());
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status("ACTIVE")
                        .planType("PREMIUM")
                        .build()));
    }

    private void stubGroupInsert() {
        when(studyGroupRepository.findByCodeIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(studyGroupRepository.save(any(StudyGroup.class))).thenAnswer(invocation -> {
            StudyGroup group = invocation.getArgument(0);
            if (group.getId() == null) {
                group.setId(UUID.randomUUID());
            }
            return group;
        });
    }

    private void stubJoinLookup(FieldTrip trip, StudyGroup group) {
        when(fieldTripRepository.findByJoinCodeIgnoreCase(JOIN_CODE)).thenReturn(Optional.of(trip));
        when(studyGroupRepository.findById(group.getId())).thenReturn(Optional.of(group));
    }

    private StudyGroup savedGroup() {
        ArgumentCaptor<StudyGroup> saved = ArgumentCaptor.forClass(StudyGroup.class);
        verify(studyGroupRepository).save(saved.capture());
        return saved.getValue();
    }

    private StudyGroupMember savedMember() {
        ArgumentCaptor<StudyGroupMember> saved = ArgumentCaptor.forClass(StudyGroupMember.class);
        verify(studyGroupMemberRepository).save(saved.capture());
        return saved.getValue();
    }

    private FieldTrip trip(FieldTripStatus status, Instant visitEnd) {
        return FieldTrip.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .siteCode("cu-chi")
                .title("Chuyến lớp 6")
                .visitStart(VISIT_START)
                .visitEnd(visitEnd)
                .questId(UUID.randomUUID())
                .campaignId(UUID.randomUUID())
                .joinCode(JOIN_CODE)
                .status(status)
                .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                .createdBy(teacherId)
                .createdAt(VISIT_START)
                .updatedAt(VISIT_START)
                .rowVersion(0L)
                .build();
    }

    private static StudyGroup group(
            UUID groupId, UUID tripId, String name, ParticipationMode mode, Integer declaredSize) {
        return StudyGroup.builder()
                .id(groupId)
                .name(name)
                .code("ABC123")
                .createdBy(UUID.randomUUID())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
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
