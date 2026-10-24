package com.histar.be.fieldtrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.quest.campaign.QuestCampaign;
import com.histar.be.quest.campaign.QuestCampaignAdminService;
import com.histar.be.quest.campaign.QuestCampaignRepository;
import com.histar.be.quest.entity.Quest;
import com.histar.be.quest.repository.QuestRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FieldTripServiceTest {

    private static final Instant VISIT_START = Instant.parse("2026-11-01T01:00:00Z");
    private static final Instant VISIT_END = Instant.parse("2026-11-01T06:00:00Z");

    @Mock
    private FieldTripRepository fieldTripRepository;

    @Mock
    private LmsAssignmentService lmsAssignmentService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private QuestRepository questRepository;

    @Mock
    private QuestCampaignRepository campaignRepository;

    @Mock
    private QuestCampaignAdminService campaignAdminService;

    private FieldTripService fieldTripService;

    private UUID teacherId;
    private UUID orgId;
    private UUID locationId;
    private UUID questId;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        FieldTripAccessService accessService =
                new FieldTripAccessService(lmsAssignmentService, organizationRepository);
        fieldTripService = new FieldTripService(
                fieldTripRepository,
                accessService,
                locationRepository,
                questRepository,
                campaignRepository,
                campaignAdminService);
        teacherId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        questId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_persistsDraftWithUnambiguousJoinCodeAndNotRequiredConfirmation() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        stubSiteQuestAndCampaign();
        when(fieldTripRepository.existsByJoinCodeIgnoreCase(anyString())).thenReturn(false);
        when(fieldTripRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            FieldTrip trip = invocation.getArgument(0);
            trip.setId(UUID.randomUUID());
            trip.setRowVersion(0L);
            return trip;
        });

        FieldTripResponse response = fieldTripService.create(teacherId, createRequest(orgId, "Cu-Chi"));

        ArgumentCaptor<FieldTrip> saved = ArgumentCaptor.forClass(FieldTrip.class);
        verify(fieldTripRepository).saveAndFlush(saved.capture());
        FieldTrip trip = saved.getValue();
        assertThat(trip.getJoinCode()).matches("[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(trip.getSiteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.NOT_REQUIRED);
        assertThat(trip.getSiteCode()).isEqualTo("cu-chi");
        assertThat(trip.getOrgId()).isEqualTo(orgId);
        assertThat(trip.getCreatedBy()).isEqualTo(teacherId);
        assertThat(trip.getQuestId()).isEqualTo(questId);
        assertThat(trip.getCampaignId()).isEqualTo(campaignId);
        assertThat(trip.getVisitStart()).isEqualTo(VISIT_START);
        assertThat(trip.getVisitEnd()).isEqualTo(VISIT_END);
        assertThat(response.joinCode()).isEqualTo(trip.getJoinCode());
        assertThat(response.status()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.NOT_REQUIRED);
        assertThat(response.rowVersion()).isZero();
        verify(campaignAdminService).requireAvailableNow(any(QuestCampaign.class), any());
    }

    @Test
    void create_retriesJoinCodeWhenStoredCodeAlreadyExistsOrUniqueConstraintConflicts() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        stubSiteQuestAndCampaign();
        when(fieldTripRepository.existsByJoinCodeIgnoreCase(anyString())).thenReturn(true, false, false);
        when(fieldTripRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key uq_field_trips_join_code_ci"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        fieldTripService.create(teacherId, createRequest(orgId, "cu-chi"));

        verify(fieldTripRepository, times(2)).saveAndFlush(any());
    }

    @Test
    void create_rejectsTeacherInWrongOrganization() {
        stubPremiumTeacher(orgId);

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(UUID.randomUUID(), "cu-chi")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền truy cập organization");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsInactiveOrganization() {
        stubPremiumTeacher(orgId);
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder().id(orgId).status("EXPIRED").build()));

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "cu-chi")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không còn hoạt động");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsUnknownSite() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        when(locationRepository.findBySiteCodeIgnoreCase("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "missing")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("di tích");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsQuestFromAnotherLocation() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi"))
                .thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").build()));
        when(questRepository.findById(questId))
                .thenReturn(Optional.of(Quest.builder().id(questId).locationId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "cu-chi")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Quest không thuộc di tích");
        verify(campaignRepository, never()).findById(any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsMissingQuest() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi"))
                .thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").build()));
        when(questRepository.findById(questId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "cu-chi")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quest");
    }

    @Test
    void create_rejectsCampaignForAnotherQuest() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi"))
                .thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").build()));
        when(questRepository.findById(questId))
                .thenReturn(Optional.of(Quest.builder().id(questId).locationId(locationId).title("Quest").build()));
        when(campaignRepository.findById(campaignId))
                .thenReturn(Optional.of(QuestCampaign.builder().id(campaignId).questId(UUID.randomUUID()).build()));

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "cu-chi")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Campaign không khả dụng cho quest");
        verify(campaignAdminService, never()).requireAvailableNow(any(), any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsUnavailableCampaign() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        stubSiteQuestAndCampaign();
        doThrow(new BusinessRuleException(
                        "Campaign chưa đạt release gate hoặc không nằm trong thời gian hoạt động"))
                .when(campaignAdminService)
                .requireAvailableNow(any(), any());

        assertThatThrownBy(() -> fieldTripService.create(teacherId, createRequest(orgId, "cu-chi")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("release gate");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void create_rejectsVisitStartAfterEnd() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        CreateFieldTripRequest request = new CreateFieldTripRequest(
                orgId, "cu-chi", "Chuyến lớp 6", VISIT_END, VISIT_START, questId, campaignId);

        assertThatThrownBy(() -> fieldTripService.create(teacherId, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("kết thúc");
        verify(locationRepository, never()).findBySiteCodeIgnoreCase(anyString());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_rejectsStaleRowVersion() {
        FieldTrip trip = storedTrip(2L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);

        assertThatThrownBy(() -> fieldTripService.update(teacherId, trip.getId(), updateRequest(), 1L))
                .isInstanceOf(NarrativeConflictException.class)
                .satisfies(ex -> assertThat(((NarrativeConflictException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VERSION_CONFLICT));
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_translatesOptimisticLockFailureToVersionConflict() {
        FieldTrip trip = storedTrip(4L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        stubSiteQuestAndCampaign();
        when(fieldTripRepository.saveAndFlush(any())).thenThrow(new OptimisticLockingFailureException("stale"));

        assertThatThrownBy(() -> fieldTripService.update(teacherId, trip.getId(), updateRequest(), 4L))
                .isInstanceOf(NarrativeConflictException.class)
                .satisfies(ex -> assertThat(((NarrativeConflictException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VERSION_CONFLICT));
    }

    @Test
    void update_rejectsTeacherFromAnotherOrganization() {
        FieldTrip trip = storedTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        UUID otherTeacher = UUID.randomUUID();
        UUID otherOrg = UUID.randomUUID();
        stubPremiumTeacher(otherTeacher, otherOrg);

        assertThatThrownBy(() -> fieldTripService.update(otherTeacher, trip.getId(), updateRequest(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("người tạo");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_rejectsSameOrgTeacherWhoDidNotCreateTheTrip() {
        FieldTrip trip = storedTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        UUID colleague = UUID.randomUUID();
        stubPremiumTeacher(colleague, orgId);

        assertThatThrownBy(() -> fieldTripService.update(colleague, trip.getId(), updateRequest(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("người tạo");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void update_allowsCreatorAndKeepsJoinCodeAndDraftState() {
        FieldTrip trip = storedTrip(3L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        stubSiteQuestAndCampaign();
        when(fieldTripRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UpdateFieldTripRequest request = new UpdateFieldTripRequest(
                "Lịch mới",
                "cu-chi",
                VISIT_START.plusSeconds(3600),
                VISIT_END.plusSeconds(3600),
                questId,
                campaignId,
                3L);

        FieldTripResponse response = fieldTripService.update(teacherId, trip.getId(), request, 3L);

        assertThat(response.title()).isEqualTo("Lịch mới");
        assertThat(response.joinCode()).isEqualTo(trip.getJoinCode());
        assertThat(response.status()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.NOT_REQUIRED);
        assertThat(response.createdBy()).isEqualTo(teacherId);
        assertThat(trip.getVisitStart()).isEqualTo(VISIT_START.plusSeconds(3600));
    }

    @Test
    void update_allowsPlatformAdminWhoDidNotCreateTheTrip() {
        FieldTrip trip = storedTrip(5L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubSiteQuestAndCampaign();
        when(fieldTripRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        FieldTripResponse response =
                fieldTripService.update(UUID.randomUUID(), trip.getId(), updateRequest(), 5L);

        assertThat(response.id()).isEqualTo(trip.getId());
        verify(lmsAssignmentService, never()).requirePremiumTeacher(any());
    }

    @Test
    void get_rejectsTeacherFromAnotherOrganization() {
        FieldTrip trip = storedTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        UUID outsider = UUID.randomUUID();
        stubPremiumTeacher(outsider, UUID.randomUUID());

        assertThatThrownBy(() -> fieldTripService.get(outsider, trip.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("organization");
    }

    @Test
    void listMine_returnsTripsCreatedByTheTeacher() {
        stubPremiumTeacher(orgId);
        stubActiveOrganization(orgId);
        FieldTrip trip = storedTrip(0L);
        when(fieldTripRepository.findByCreatedByOrderByCreatedAtDesc(teacherId)).thenReturn(List.of(trip));

        assertThat(fieldTripService.listMine(teacherId)).extracting(FieldTripResponse::id).containsExactly(trip.getId());
    }

    private void stubPremiumTeacher(UUID organizationId) {
        stubPremiumTeacher(teacherId, organizationId);
    }

    private void stubPremiumTeacher(UUID userId, UUID organizationId) {
        when(lmsAssignmentService.requirePremiumTeacher(userId))
                .thenReturn(Profile.builder().id(userId).orgId(organizationId).build());
    }

    private void stubActiveOrganization(UUID organizationId) {
        when(organizationRepository.findById(organizationId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(organizationId)
                        .status("ACTIVE")
                        .planType("PREMIUM")
                        .build()));
    }

    private void stubSiteQuestAndCampaign() {
        when(locationRepository.findBySiteCodeIgnoreCase("cu-chi"))
                .thenReturn(Optional.of(Location.builder().id(locationId).siteCode("cu-chi").name("Củ Chi").build()));
        when(questRepository.findById(questId))
                .thenReturn(Optional.of(Quest.builder()
                        .id(questId)
                        .locationId(locationId)
                        .title("Hành trình địa đạo")
                        .build()));
        when(campaignRepository.findById(campaignId))
                .thenReturn(Optional.of(QuestCampaign.builder()
                        .id(campaignId)
                        .questId(questId)
                        .title("Chiến dịch tháng 11")
                        .build()));
    }

    private CreateFieldTripRequest createRequest(UUID organizationId, String siteCode) {
        return new CreateFieldTripRequest(
                organizationId, siteCode, "Chuyến lớp 6", VISIT_START, VISIT_END, questId, campaignId);
    }

    private UpdateFieldTripRequest updateRequest() {
        return new UpdateFieldTripRequest(
                "Chuyến lớp 6", "cu-chi", VISIT_START, VISIT_END, questId, campaignId, 0L);
    }

    private FieldTrip storedTrip(long rowVersion) {
        return FieldTrip.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .siteCode("cu-chi")
                .title("Chuyến lớp 6")
                .visitStart(VISIT_START)
                .visitEnd(VISIT_END)
                .questId(questId)
                .campaignId(campaignId)
                .joinCode("ABCDEFGH")
                .status(FieldTripStatus.DRAFT)
                .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                .createdBy(teacherId)
                .createdAt(VISIT_START)
                .updatedAt(VISIT_START)
                .rowVersion(rowVersion)
                .build();
    }
}
