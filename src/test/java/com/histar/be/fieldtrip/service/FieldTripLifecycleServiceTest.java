package com.histar.be.fieldtrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.FieldTripResponse;
import com.histar.be.fieldtrip.dto.SiteFieldTripScheduleResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.lms.entity.Assignment;
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.entity.SiteWorkspaceSettings;
import com.histar.be.siteworkspace.repository.SiteWorkspaceSettingsRepository;
import com.histar.be.siteworkspace.service.SiteWorkspaceAccessService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class FieldTripLifecycleServiceTest {

    private static final Instant VISIT_START = Instant.parse("2026-11-01T01:00:00Z");
    private static final Instant VISIT_END = Instant.parse("2026-11-01T06:00:00Z");
    private static final String SITE = "cu-chi";
    private static final Set<SiteRole> SITE_MANAGERS = Set.of(SiteRole.SITE_MANAGER);

    @Mock
    private FieldTripRepository fieldTripRepository;

    @Mock
    private LmsAssignmentService lmsAssignmentService;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private SiteWorkspaceAccessService siteWorkspaceAccessService;

    @Mock
    private SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private AdminAuditService adminAuditService;

    private FieldTripLifecycleService service;

    private UUID teacherId;
    private UUID orgId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        service = new FieldTripLifecycleService(
                fieldTripRepository,
                new FieldTripAccessService(lmsAssignmentService, organizationRepository),
                siteWorkspaceAccessService,
                siteWorkspaceSettingsRepository,
                lmsAssignmentService,
                studyGroupRepository,
                adminAuditService);
        teacherId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        managerId = UUID.randomUUID();
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestConfirmation_movesDraftToPendingWhenSiteRequiresIt() {
        FieldTrip trip = draftTrip(4L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.requestConfirmation(teacherId, trip.getId(), 4L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.PENDING_SITE);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.PENDING);
        assertThat(response.rowVersion()).isEqualTo(5L);
        assertThat(response.joinCode()).isEqualTo("ABCDEFGH");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.PENDING_SITE);
        assertAudited(
                teacherId,
                "REQUEST_CONFIRMATION",
                trip,
                FieldTripStatus.DRAFT,
                FieldTripStatus.PENDING_SITE,
                SiteConfirmationStatus.NOT_REQUIRED,
                SiteConfirmationStatus.PENDING,
                4L,
                5L);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void requestConfirmation_rejectsWhenSettingsAreMissing() {
        FieldTrip trip = draftTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestConfirmation(teacherId, trip.getId(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Di tích không yêu cầu xác nhận chuyến");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(trip.getSiteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.NOT_REQUIRED);
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestConfirmation_rejectsWhenConfirmationIsNotRequired() {
        FieldTrip trip = draftTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(false);

        assertThatThrownBy(() -> service.requestConfirmation(teacherId, trip.getId(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Di tích không yêu cầu xác nhận chuyến");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "DRAFT")
    void requestConfirmation_rejectsAnythingOtherThanDraft(FieldTripStatus status) {
        FieldTrip trip = draftTrip(2L);
        trip.setStatus(status);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);

        assertThatThrownBy(() -> service.requestConfirmation(teacherId, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chỉ chuyến nháp mới được gửi xác nhận");
        assertThat(trip.getStatus()).isEqualTo(status);
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestConfirmation_rejectsTeacherWhoDidNotCreateTheTrip() {
        FieldTrip trip = draftTrip(2L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        UUID otherTeacher = UUID.randomUUID();
        when(lmsAssignmentService.requirePremiumTeacher(otherTeacher))
                .thenReturn(Profile.builder().id(otherTeacher).orgId(orgId).build());

        assertThatThrownBy(() -> service.requestConfirmation(otherTeacher, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("người tạo");
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void requestConfirmation_allowsPlatformAdminWhoDidNotCreateTheTrip() {
        FieldTrip trip = draftTrip(3L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubConfirmationRequired(true);
        stubSaveIncrementsVersion();
        authenticateAdmin();

        FieldTripResponse response = service.requestConfirmation(UUID.randomUUID(), trip.getId(), 3L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.PENDING_SITE);
        verify(lmsAssignmentService, never()).requirePremiumTeacher(any());
    }

    @Test
    void requestConfirmation_rejectsStaleVersionBeforeSaving() {
        FieldTrip trip = draftTrip(6L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();

        assertVersionConflict(() -> service.requestConfirmation(teacherId, trip.getId(), 5L));
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
        verify(fieldTripRepository, never()).saveAndFlush(any());
        verify(adminAuditService, never()).append(any());
    }

    @Test
    void confirm_movesPendingTripToConfirmedForTheSiteManager() {
        FieldTrip trip = pendingTrip(2L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.confirm(managerId, "Cu-Chi", trip.getId(), 2L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.CONFIRMED);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.CONFIRMED);
        assertThat(response.rowVersion()).isEqualTo(3L);
        assertThat(trip.getStatus()).isNotEqualTo(FieldTripStatus.ACTIVE);
        verify(siteWorkspaceAccessService).requireAny(managerId, "Cu-Chi", SITE_MANAGERS);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        assertAudited(
                managerId,
                "CONFIRM",
                trip,
                FieldTripStatus.PENDING_SITE,
                FieldTripStatus.CONFIRMED,
                SiteConfirmationStatus.PENDING,
                SiteConfirmationStatus.CONFIRMED,
                2L,
                3L);
    }

    @Test
    void confirm_rejectsWrongSiteManagerBeforeLoadingTheTrip() {
        UUID intruder = UUID.randomUUID();
        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(siteWorkspaceAccessService)
                .requireAny(intruder, SITE, SITE_MANAGERS);

        assertThatThrownBy(() -> service.confirm(intruder, SITE, UUID.randomUUID(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Không có quyền truy cập site workspace này");
        verify(fieldTripRepository, never()).findById(any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void confirm_rejectsTripThatBelongsToAnotherSite() {
        FieldTrip trip = pendingTrip(2L);
        trip.setSiteCode("hoi-an");
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.confirm(managerId, SITE, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Không có quyền truy cập site workspace này");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.PENDING_SITE);
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "PENDING_SITE")
    void confirm_rejectsTripThatIsNotWaitingForTheSite(FieldTripStatus status) {
        FieldTrip trip = pendingTrip(2L);
        trip.setStatus(status);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.confirm(managerId, SITE, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chuyến đi không đang chờ xác nhận");
        verify(fieldTripRepository, never()).saveAndFlush(any());
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void confirm_rejectsPendingTripWhoseConfirmationIsNotPending() {
        FieldTrip trip = pendingTrip(2L);
        trip.setSiteConfirmationStatus(SiteConfirmationStatus.DECLINED);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> service.confirm(managerId, SITE, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chuyến đi không đang chờ xác nhận");
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void decline_returnsTripToDraftAndStoresTheNoteWithoutStarting() {
        FieldTrip trip = pendingTrip(8L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubSaveIncrementsVersion();
        String note = "  Học sinh a@school.edu không đến  ";

        FieldTripResponse response = service.decline(managerId, SITE, trip.getId(), 8L, note);

        assertThat(response.status()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.DECLINED);
        assertThat(response.siteNote()).isEqualTo("Học sinh a@school.edu không đến");
        assertThat(response.rowVersion()).isEqualTo(9L);
        assertThat(trip.getStatus()).isNotEqualTo(FieldTripStatus.ACTIVE);
        verify(siteWorkspaceAccessService).requireAny(managerId, SITE, SITE_MANAGERS);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(adminAuditService).append(captor.capture());
        AdminAuditCommand command = captor.getValue();
        assertThat(command.action()).isEqualTo("DECLINE");
        assertThat(command.reason()).isNull();
        assertThat(command.idempotencyKey()).isNull();
        Map<String, Object> metadata = auditMetadata(command);
        assertThat(metadata.values())
                .map(String::valueOf)
                .noneMatch(value -> value.contains("@") || value.contains("Học sinh"));
        assertThat(metadata)
                .containsEntry("fromStatus", FieldTripStatus.PENDING_SITE.name())
                .containsEntry("toStatus", FieldTripStatus.DRAFT.name())
                .containsEntry("toSiteConfirmationStatus", SiteConfirmationStatus.DECLINED.name());
    }

    @Test
    void decline_rejectsWrongSiteManager() {
        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(siteWorkspaceAccessService)
                .requireAny(managerId, SITE, SITE_MANAGERS);

        assertThatThrownBy(() -> service.decline(managerId, SITE, UUID.randomUUID(), 1L, "trùng lịch"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Không có quyền truy cập site workspace này");
        verify(fieldTripRepository, never()).findById(any());
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void requestConfirmation_afterDeclineCanBeSentAgain() {
        FieldTrip trip = draftTrip(9L);
        trip.setSiteConfirmationStatus(SiteConfirmationStatus.DECLINED);
        trip.setSiteNote("trùng đoàn");
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.requestConfirmation(teacherId, trip.getId(), 9L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.PENDING_SITE);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.PENDING);
        assertThat(response.siteNote()).isNull();
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @Test
    void start_fromDraftWhenConfirmationIsNotRequiredCreatesOneAssignmentThenActivates() {
        FieldTrip trip = draftTrip(1L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE)).thenReturn(Optional.empty());
        Assignment assignment = Assignment.builder().id(UUID.randomUUID()).title(trip.getTitle()).build();
        when(lmsAssignmentService.ensureForFieldTrip(trip)).thenAnswer(invocation -> {
            assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
            return assignment;
        });
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.start(teacherId, trip.getId(), 1L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.ACTIVE);
        assertThat(response.rowVersion()).isEqualTo(2L);
        assertThat(trip.getSiteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.NOT_REQUIRED);
        InOrder order = inOrder(lmsAssignmentService, fieldTripRepository);
        order.verify(lmsAssignmentService).ensureForFieldTrip(trip);
        order.verify(fieldTripRepository).saveAndFlush(trip);
        assertAudited(
                teacherId,
                "START",
                trip,
                FieldTripStatus.DRAFT,
                FieldTripStatus.ACTIVE,
                SiteConfirmationStatus.NOT_REQUIRED,
                SiteConfirmationStatus.NOT_REQUIRED,
                1L,
                2L);
    }

    @Test
    void start_fromConfirmedCreatesAssignmentThenActivatesInsideTheSameCall() {
        FieldTrip trip = confirmedTrip(4L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);
        AtomicInteger ensured = new AtomicInteger();
        when(lmsAssignmentService.ensureForFieldTrip(trip)).thenAnswer(invocation -> {
            if (ensured.getAndIncrement() == 0) {
                assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.CONFIRMED);
            } else {
                assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.ACTIVE);
            }
            return Assignment.builder().id(UUID.randomUUID()).title(trip.getTitle()).build();
        });
        stubSaveIncrementsVersion();

        FieldTripResponse first = service.start(teacherId, trip.getId(), 4L);
        FieldTripResponse repeat = service.start(teacherId, trip.getId(), 4L);

        assertThat(first.status()).isEqualTo(FieldTripStatus.ACTIVE);
        assertThat(repeat.status()).isEqualTo(FieldTripStatus.ACTIVE);
        assertThat(repeat.rowVersion()).isEqualTo(first.rowVersion()).isEqualTo(5L);
        verify(lmsAssignmentService, times(2)).ensureForFieldTrip(trip);
        verify(fieldTripRepository, times(1)).saveAndFlush(any());
        verify(adminAuditService, times(1)).append(any());
    }

    @ParameterizedTest
    @EnumSource(SiteConfirmationStatus.class)
    void start_rejectsDraftWhenSiteRequiresConfirmation(SiteConfirmationStatus confirmation) {
        FieldTrip trip = draftTrip(3L);
        trip.setSiteConfirmationStatus(confirmation);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);

        assertThatThrownBy(() -> service.start(teacherId, trip.getId(), 3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chuyến đi chưa thể bắt đầu");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void start_afterDeclineDoesNotActivate() {
        FieldTrip trip = draftTrip(9L);
        trip.setSiteConfirmationStatus(SiteConfirmationStatus.DECLINED);
        trip.setSiteNote("trùng đoàn");
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);

        assertThatThrownBy(() -> service.start(teacherId, trip.getId(), 9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chuyến đi chưa thể bắt đầu");
        assertThat(trip.getStatus()).isEqualTo(FieldTripStatus.DRAFT);
        assertThat(trip.getSiteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.DECLINED);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"PENDING_SITE", "COMPLETED", "CANCELLED"})
    void start_rejectsWaitingClosedOrCancelledTrips(FieldTripStatus status) {
        FieldTrip trip = draftTrip(3L);
        trip.setStatus(status);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();

        assertThatThrownBy(() -> service.start(teacherId, trip.getId(), 3L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chuyến đi chưa thể bắt đầu");
        assertThat(trip.getStatus()).isEqualTo(status);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void start_rejectsStaleVersionBeforeCreatingAnAssignment() {
        FieldTrip trip = confirmedTrip(7L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();

        assertVersionConflict(() -> service.start(teacherId, trip.getId(), 6L));
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void start_mapsOptimisticLockToVersionConflictAfterEnsuringAssignment() {
        FieldTrip trip = confirmedTrip(4L);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubConfirmationRequired(true);
        when(lmsAssignmentService.ensureForFieldTrip(trip))
                .thenReturn(Assignment.builder().id(UUID.randomUUID()).title(trip.getTitle()).build());
        when(fieldTripRepository.saveAndFlush(trip)).thenThrow(new OptimisticLockingFailureException("stale"));

        assertVersionConflict(() -> service.start(teacherId, trip.getId(), 4L));
        verify(lmsAssignmentService).ensureForFieldTrip(trip);
        verify(adminAuditService, never()).append(any());
    }

    @Test
    void start_ofAnActiveTripIsIdempotentAndStillEnsuresTheAssignment() {
        FieldTrip trip = confirmedTrip(5L);
        trip.setStatus(FieldTripStatus.ACTIVE);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        when(lmsAssignmentService.ensureForFieldTrip(trip))
                .thenReturn(Assignment.builder().id(UUID.randomUUID()).title(trip.getTitle()).build());

        FieldTripResponse response = service.start(teacherId, trip.getId(), 0L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.ACTIVE);
        assertThat(response.rowVersion()).isEqualTo(5L);
        verify(fieldTripRepository, never()).saveAndFlush(any());
        verify(adminAuditService, never()).append(any());
        verify(siteWorkspaceSettingsRepository, never()).findBySiteCodeIgnoreCase(anyString());
    }

    @Test
    void complete_movesActiveTripToCompletedWithoutCreatingAssignmentOrSnapshotWork() {
        FieldTrip trip = confirmedTrip(5L);
        trip.setStatus(FieldTripStatus.ACTIVE);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.complete(teacherId, trip.getId(), 5L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.COMPLETED);
        assertThat(response.siteConfirmationStatus()).isEqualTo(SiteConfirmationStatus.CONFIRMED);
        assertThat(response.rowVersion()).isEqualTo(6L);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
        assertAudited(
                teacherId,
                "COMPLETE",
                trip,
                FieldTripStatus.ACTIVE,
                FieldTripStatus.COMPLETED,
                SiteConfirmationStatus.CONFIRMED,
                SiteConfirmationStatus.CONFIRMED,
                5L,
                6L);
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "ACTIVE")
    void complete_rejectsAnythingOtherThanActive(FieldTripStatus status) {
        FieldTrip trip = draftTrip(1L);
        trip.setStatus(status);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();

        assertThatThrownBy(() -> service.complete(teacherId, trip.getId(), 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chỉ chuyến đang diễn ra mới được kết thúc");
        assertThat(trip.getStatus()).isEqualTo(status);
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"DRAFT", "PENDING_SITE", "CONFIRMED"})
    void cancel_cancelsOnlyTripsThatHaveNotStarted(FieldTripStatus status) {
        FieldTrip trip = draftTrip(2L);
        trip.setStatus(status);
        trip.setSiteConfirmationStatus(
                status == FieldTripStatus.DRAFT
                        ? SiteConfirmationStatus.NOT_REQUIRED
                        : SiteConfirmationStatus.PENDING);
        if (status == FieldTripStatus.CONFIRMED) {
            trip.setSiteConfirmationStatus(SiteConfirmationStatus.CONFIRMED);
        }
        SiteConfirmationStatus confirmation = trip.getSiteConfirmationStatus();
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();
        stubSaveIncrementsVersion();

        FieldTripResponse response = service.cancel(teacherId, trip.getId(), 2L);

        assertThat(response.status()).isEqualTo(FieldTripStatus.CANCELLED);
        assertThat(response.siteConfirmationStatus()).isEqualTo(confirmation);
        assertThat(response.rowVersion()).isEqualTo(3L);
        verify(lmsAssignmentService, never()).ensureForFieldTrip(any());
    }

    @ParameterizedTest
    @EnumSource(value = FieldTripStatus.class, names = {"ACTIVE", "COMPLETED", "CANCELLED"})
    void cancel_rejectsStartedOrFinishedTrips(FieldTripStatus status) {
        FieldTrip trip = draftTrip(2L);
        trip.setStatus(status);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        stubTeacher();

        assertThatThrownBy(() -> service.cancel(teacherId, trip.getId(), 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chỉ hủy được chuyến chưa bắt đầu");
        assertThat(trip.getStatus()).isEqualTo(status);
        verify(fieldTripRepository, never()).saveAndFlush(any());
    }

    @Test
    void mutations_rejectAnUnknownTrip() {
        when(fieldTripRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(teacherId, UUID.randomUUID(), 0L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy chuyến đi");
    }

    @Test
    void listForSite_returnsScheduleAndGroupCountsWithoutStudentFields() {
        FieldTrip earlier = pendingTrip(1L);
        FieldTrip later = draftTrip(2L);
        later.setVisitStart(VISIT_START.plusSeconds(3600));
        later.setTitle("Chuyến chiều");
        when(fieldTripRepository.findBySiteCodeIgnoreCaseOrderByVisitStartAsc(SITE))
                .thenReturn(List.of(earlier, later));
        when(studyGroupRepository.countByFieldTripId(earlier.getId())).thenReturn(2L);
        when(studyGroupRepository.countByFieldTripId(later.getId())).thenReturn(0L);

        List<SiteFieldTripScheduleResponse> schedule = service.listForSite(managerId, SITE);

        assertThat(schedule)
                .containsExactly(
                        new SiteFieldTripScheduleResponse(
                                earlier.getId(),
                                earlier.getTitle(),
                                earlier.getVisitStart(),
                                earlier.getVisitEnd(),
                                FieldTripStatus.PENDING_SITE,
                                SiteConfirmationStatus.PENDING,
                                2L),
                        new SiteFieldTripScheduleResponse(
                                later.getId(),
                                "Chuyến chiều",
                                later.getVisitStart(),
                                later.getVisitEnd(),
                                FieldTripStatus.DRAFT,
                                SiteConfirmationStatus.NOT_REQUIRED,
                                0L));
        assertThat(SiteFieldTripScheduleResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly(
                        "id",
                        "title",
                        "visitStart",
                        "visitEnd",
                        "status",
                        "siteConfirmationStatus",
                        "groupCount");
        InOrder order = inOrder(siteWorkspaceAccessService, fieldTripRepository);
        order.verify(siteWorkspaceAccessService).requireAny(managerId, SITE, SITE_MANAGERS);
        order.verify(fieldTripRepository).findBySiteCodeIgnoreCaseOrderByVisitStartAsc(SITE);
        verify(lmsAssignmentService, never()).requirePremiumTeacher(any());
    }

    @Test
    void listForSite_rejectsWrongSiteManagerBeforeReadingTrips() {
        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(siteWorkspaceAccessService)
                .requireAny(managerId, SITE, SITE_MANAGERS);

        assertThatThrownBy(() -> service.listForSite(managerId, SITE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Không có quyền truy cập site workspace này");
        verify(fieldTripRepository, never()).findBySiteCodeIgnoreCaseOrderByVisitStartAsc(anyString());
    }

    private void assertVersionConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(NarrativeConflictException.class)
                .satisfies(ex -> assertThat(((NarrativeConflictException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.VERSION_CONFLICT))
                .hasMessage("Chuyến đi đã được cập nhật, vui lòng tải lại");
    }

    private void assertAudited(
            UUID actorId,
            String action,
            FieldTrip trip,
            FieldTripStatus fromStatus,
            FieldTripStatus toStatus,
            SiteConfirmationStatus fromConfirmation,
            SiteConfirmationStatus toConfirmation,
            long oldVersion,
            long newVersion) {
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(adminAuditService).append(captor.capture());
        AdminAuditCommand command = captor.getValue();
        assertThat(command.actorId()).isEqualTo(actorId);
        assertThat(command.entityType()).isEqualTo("FIELD_TRIP");
        assertThat(command.entityId()).isEqualTo(trip.getId());
        assertThat(command.action()).isEqualTo(action);
        assertThat(command.oldRowVersion()).isEqualTo(oldVersion);
        assertThat(command.newRowVersion()).isEqualTo(newVersion);
        assertThat(command.reason()).isNull();
        assertThat(command.idempotencyKey()).isNull();
        Map<String, Object> metadata = auditMetadata(command);
        assertThat(metadata.keySet())
                .containsExactlyInAnyOrder(
                        "tripId",
                        "siteCode",
                        "fromStatus",
                        "toStatus",
                        "fromSiteConfirmationStatus",
                        "toSiteConfirmationStatus");
        assertThat(metadata)
                .containsEntry("tripId", trip.getId().toString())
                .containsEntry("siteCode", trip.getSiteCode())
                .containsEntry("fromStatus", fromStatus.name())
                .containsEntry("toStatus", toStatus.name())
                .containsEntry("fromSiteConfirmationStatus", fromConfirmation.name())
                .containsEntry("toSiteConfirmationStatus", toConfirmation.name());
    }

    private static Map<String, Object> auditMetadata(AdminAuditCommand command) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        command.metadata().forEach((key, value) -> metadata.put(key, value));
        return metadata;
    }

    private void stubTeacher() {
        stubTeacher(teacherId);
    }

    private void stubTeacher(UUID userId) {
        when(lmsAssignmentService.requirePremiumTeacher(userId))
                .thenReturn(Profile.builder().id(userId).orgId(orgId).build());
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status("ACTIVE")
                        .planType("PREMIUM")
                        .build()));
    }

    private void stubConfirmationRequired(boolean required) {
        when(siteWorkspaceSettingsRepository.findBySiteCodeIgnoreCase(SITE))
                .thenReturn(Optional.of(SiteWorkspaceSettings.builder()
                        .siteCode(SITE)
                        .requiresTripConfirmation(required)
                        .build()));
    }

    private void stubSaveIncrementsVersion() {
        when(fieldTripRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            FieldTrip trip = invocation.getArgument(0);
            trip.setRowVersion(trip.getRowVersion() + 1);
            return trip;
        });
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private FieldTrip draftTrip(long rowVersion) {
        return FieldTrip.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .siteCode(SITE)
                .title("Chuyến lớp 6")
                .visitStart(VISIT_START)
                .visitEnd(VISIT_END)
                .questId(UUID.randomUUID())
                .campaignId(UUID.randomUUID())
                .joinCode("ABCDEFGH")
                .status(FieldTripStatus.DRAFT)
                .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                .createdBy(teacherId)
                .createdAt(VISIT_START)
                .updatedAt(VISIT_START)
                .rowVersion(rowVersion)
                .build();
    }

    private FieldTrip pendingTrip(long rowVersion) {
        FieldTrip trip = draftTrip(rowVersion);
        trip.setStatus(FieldTripStatus.PENDING_SITE);
        trip.setSiteConfirmationStatus(SiteConfirmationStatus.PENDING);
        return trip;
    }

    private FieldTrip confirmedTrip(long rowVersion) {
        FieldTrip trip = draftTrip(rowVersion);
        trip.setStatus(FieldTripStatus.CONFIRMED);
        trip.setSiteConfirmationStatus(SiteConfirmationStatus.CONFIRMED);
        return trip;
    }
}
