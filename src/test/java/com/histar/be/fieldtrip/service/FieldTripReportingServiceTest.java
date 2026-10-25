package com.histar.be.fieldtrip.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.billing.service.BillingService;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.FieldTripLiveBoardResponse;
import com.histar.be.fieldtrip.dto.FieldTripLiveBoardResponse.Row;
import com.histar.be.fieldtrip.dto.FieldTripReportResponse;
import com.histar.be.fieldtrip.dto.FieldTripReportResponse.Group;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripReportSnapshot;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.entity.SiteConfirmationStatus;
import com.histar.be.fieldtrip.repository.FieldTripReportSnapshotRepository;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.lms.service.LmsAssignmentService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FieldTripReportingServiceTest {

    private static final Instant TRIP_A_START = Instant.parse("2026-11-01T01:00:00Z");
    private static final Instant TRIP_A_END = Instant.parse("2026-11-01T06:00:00Z");
    private static final Instant TRIP_B_START = Instant.parse("2026-11-02T01:00:00Z");
    private static final Instant TRIP_B_END = Instant.parse("2026-11-02T06:00:00Z");
    private static final String SITE_A = "cu-chi";
    private static final String SITE_B = "hoi-an";

    @Mock
    private FieldTripRepository fieldTripRepository;

    @Mock
    private FieldTripReportSnapshotRepository snapshotRepository;

    @Mock
    private FieldTripLifecycleService lifecycleService;

    @Mock
    private StudyGroupRepository studyGroupRepository;

    @Mock
    private StudyGroupMemberRepository studyGroupMemberRepository;

    @Mock
    private CheckinRepository checkinRepository;

    @Mock
    private UserQuestProgressRepository userQuestProgressRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private LmsAssignmentService lmsAssignmentService;

    @Mock
    private OrganizationRepository organizationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<UUID, FieldTripReportSnapshot> snapshots = new HashMap<>();
    private final List<Checkin> checkins = new ArrayList<>();
    private final List<UserQuestProgress> progress = new ArrayList<>();

    private FieldTripReportAssembler reportAssembler;
    private FieldTripReportingService service;

    private UUID teacherId;
    private UUID orgId;
    private UUID questId;
    private UUID campaignId;
    private UUID locationAId;
    private UUID locationBId;
    private UUID student1;
    private UUID student2;
    private UUID device1;
    private UUID device2;

    @BeforeEach
    void setUp() {
        reportAssembler = new FieldTripReportAssembler(
                studyGroupRepository,
                studyGroupMemberRepository,
                checkinRepository,
                userQuestProgressRepository,
                locationRepository,
                objectMapper);
        service = new FieldTripReportingService(
                fieldTripRepository,
                snapshotRepository,
                lifecycleService,
                new FieldTripAccessService(lmsAssignmentService, organizationRepository),
                studyGroupRepository,
                profileRepository,
                reportAssembler);
        teacherId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        questId = UUID.randomUUID();
        campaignId = UUID.randomUUID();
        locationAId = UUID.randomUUID();
        locationBId = UUID.randomUUID();
        student1 = UUID.randomUUID();
        student2 = UUID.randomUUID();
        device1 = UUID.randomUUID();
        device2 = UUID.randomUUID();
    }

    @Test
    void twoTripsSharingQuestAndUsersStayIsolatedAndSharedDeviceUsesDeclaredSize() throws Exception {
        FieldTrip tripA = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 5L);
        FieldTrip tripB = activeTrip("Chuyến B", SITE_B, TRIP_B_START, TRIP_B_END, 3L);
        StudyGroup classGroup = group(tripA, "Lop A", ParticipationMode.INDIVIDUAL, 30);
        StudyGroup sharedGroup = group(tripA, "May chung", ParticipationMode.SHARED_DEVICE, 12);
        StudyGroup otherGroup = group(tripB, "Lop B", ParticipationMode.INDIVIDUAL, 99);

        stubTeacher(teacherId);
        stubTrips(tripA, tripB);
        stubGroups(tripA, List.of(classGroup, sharedGroup), tripB, List.of(otherGroup));
        stubMembers(classGroup, List.of(student1, student2), 2);
        stubMembers(sharedGroup, List.of(device1, device2), 2);
        stubMembers(otherGroup, List.of(student1), 1);
        stubLocations();
        stubProfiles();
        stubScopedCheckins();
        stubScopedQuestProgress();
        stubSnapshotStore();
        stubComplete(tripA, 5L);
        stubComplete(tripB, 3L);

        checkins.add(checkin(student1, locationAId, "ST01", 5, TRIP_A_START.plusSeconds(3600)));
        checkins.add(checkin(student2, locationAId, "ST02", 3, TRIP_A_START.plusSeconds(7200)));
        checkins.add(checkin(student1, locationBId, "ST09", 100, TRIP_B_START.plusSeconds(3600)));
        checkins.add(checkin(student1, locationAId, "ST03", 50, TRIP_A_START.minusSeconds(3600)));
        checkins.add(checkin(student1, locationAId, "ST08", 80, TRIP_B_START.plusSeconds(7200)));
        checkins.add(checkin(device1, locationAId, "ST01", 7, TRIP_A_START.plusSeconds(1800)));
        checkins.add(checkin(device2, locationAId, "ST04", 4, TRIP_A_START.plusSeconds(5400)));

        progress.add(questProgress(student1, "completed", TRIP_A_START.plusSeconds(10_000)));
        progress.add(questProgress(device1, "completed", TRIP_A_START.plusSeconds(11_000)));
        progress.add(questProgress(device2, "completed", TRIP_B_START.plusSeconds(1800)));
        progress.add(questProgress(student2, "in_progress", null));

        assertThatThrownBy(() -> service.report(teacherId, tripA.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Báo cáo chỉ có sau khi kết thúc chuyến");

        FieldTripLiveBoardResponse boardA = service.liveBoard(teacherId, tripA.getId());
        FieldTripLiveBoardResponse boardB = service.liveBoard(teacherId, tripB.getId());

        assertThat(boardA.rows()).extracting(Row::groupId).containsOnly(classGroup.getId(), sharedGroup.getId());
        assertThat(boardA.rows()).extracting(Row::groupId).doesNotContain(otherGroup.getId());
        assertThat(boardB.rows()).extracting(Row::groupId).containsExactly(otherGroup.getId());

        Row an = rowForUser(boardA, student1);
        Row binh = rowForUser(boardA, student2);
        assertThat(an.displayName()).isEqualTo("An");
        assertThat(an.stationsCompleted()).isEqualTo(1);
        assertThat(an.points()).isEqualTo(5);
        assertThat(binh.displayName()).isEqualTo("Binh");
        assertThat(binh.stationsCompleted()).isEqualTo(1);
        assertThat(binh.points()).isEqualTo(3);
        assertThat(boardA.rows().stream().filter(row -> row.participationMode() == ParticipationMode.INDIVIDUAL))
                .hasSize(2);

        List<Row> sharedRows = boardA.rows().stream()
                .filter(row -> row.groupId().equals(sharedGroup.getId()))
                .toList();
        assertThat(sharedRows).hasSize(1);
        assertThat(sharedRows.get(0).participationMode()).isEqualTo(ParticipationMode.SHARED_DEVICE);
        assertThat(sharedRows.get(0).headcount()).isEqualTo(12);
        assertThat(sharedRows.get(0).userId()).isNull();
        assertThat(sharedRows.get(0).displayName()).isEqualTo("May chung");
        assertThat(sharedRows.get(0).stationsCompleted()).isEqualTo(2);
        assertThat(sharedRows.get(0).points()).isEqualTo(11);

        Row other = boardB.rows().get(0);
        assertThat(other.userId()).isEqualTo(student1);
        assertThat(other.displayName()).isEqualTo("An");
        assertThat(other.headcount()).isEqualTo(1);
        assertThat(other.stationsCompleted()).isEqualTo(1);
        assertThat(other.points()).isEqualTo(100);

        assertNoPersonalData(objectMapper.writeValueAsString(boardA));
        assertNoPersonalData(objectMapper.writeValueAsString(boardB));

        FieldTripReportResponse reportA = service.completeAndSnapshot(teacherId, tripA.getId(), 5L);
        FieldTripReportResponse reportB = service.completeAndSnapshot(teacherId, tripB.getId(), 3L);

        assertThat(reportA.status()).isEqualTo(FieldTripStatus.COMPLETED);
        assertThat(reportA.questId()).isEqualTo(questId);
        assertThat(reportA.campaignId()).isEqualTo(campaignId);
        assertThat(reportA.groups()).extracting(Group::groupId).containsExactlyInAnyOrder(classGroup.getId(), sharedGroup.getId());
        assertThat(reportA.groups()).extracting(Group::groupId).doesNotContain(otherGroup.getId());

        Group classReport = groupReport(reportA, classGroup.getId());
        assertThat(classReport.name()).isEqualTo("Lop A");
        assertThat(classReport.participationMode()).isEqualTo(ParticipationMode.INDIVIDUAL);
        assertThat(classReport.headcount()).isEqualTo(2);
        assertThat(classReport.stationsCompleted()).isEqualTo(2);
        assertThat(classReport.questCompletions()).isEqualTo(1);
        assertThat(classReport.points()).isEqualTo(8);

        Group sharedReport = groupReport(reportA, sharedGroup.getId());
        assertThat(sharedReport.name()).isEqualTo("May chung");
        assertThat(sharedReport.participationMode()).isEqualTo(ParticipationMode.SHARED_DEVICE);
        assertThat(sharedReport.headcount()).isEqualTo(12);
        assertThat(sharedReport.stationsCompleted()).isEqualTo(2);
        assertThat(sharedReport.questCompletions()).isEqualTo(1);
        assertThat(sharedReport.points()).isEqualTo(11);

        Group otherReport = groupReport(reportB, otherGroup.getId());
        assertThat(otherReport.headcount()).isEqualTo(1);
        assertThat(otherReport.stationsCompleted()).isEqualTo(1);
        assertThat(otherReport.questCompletions()).isZero();
        assertThat(otherReport.points()).isEqualTo(100);
        assertThat(reportB.groups()).extracting(Group::groupId).doesNotContain(classGroup.getId(), sharedGroup.getId());

        for (FieldTripReportSnapshot snapshot : snapshots.values()) {
            assertNoPersonalData(snapshot.getReportJson());
            JsonNode groups = objectMapper.readTree(snapshot.getReportJson()).get("groups");
            assertThat(groups).isNotEmpty();
            for (JsonNode group : groups) {
                assertThat(fieldNames(group))
                        .containsExactlyInAnyOrder(
                                "groupId",
                                "name",
                                "participationMode",
                                "headcount",
                                "stationsCompleted",
                                "questCompletions",
                                "points");
            }
        }

        checkins.add(checkin(student1, locationAId, "ST05", 20, TRIP_A_START.plusSeconds(4000)));
        FieldTripLiveBoardResponse boardAfter = service.liveBoard(teacherId, tripA.getId());
        assertThat(rowForUser(boardAfter, student1).points()).isEqualTo(25);

        FieldTripReportResponse reread = service.report(teacherId, tripA.getId());
        FieldTripReportResponse repeatedComplete = service.completeAndSnapshot(teacherId, tripA.getId(), 5L);
        assertThat(reread).isEqualTo(reportA);
        assertThat(repeatedComplete).isEqualTo(reportA);
        assertThat(groupReport(reread, classGroup.getId()).points()).isEqualTo(8);
        assertThat(groupReport(reread, classGroup.getId()).stationsCompleted()).isEqualTo(2);

        verify(lifecycleService, times(1)).complete(teacherId, tripA.getId(), 5L);
        verify(lifecycleService, times(1)).complete(teacherId, tripB.getId(), 3L);
        verify(snapshotRepository, times(2)).save(any());
        verify(checkinRepository, never()).findAll();
        verify(userQuestProgressRepository, never()).findAll();
        verify(checkinRepository, never()).findByUserIdInAndCreatedAtGreaterThanEqual(any(), any());
    }

    @Test
    void sameSiteOverlappingTripsKeepEachGroupsMetricsAndIgnoreQuestRowsWithoutPresence() {
        Instant overlap = Instant.parse("2026-11-01T04:30:00Z");
        Instant tripBStart = Instant.parse("2026-11-01T04:00:00Z");
        Instant tripBEnd = Instant.parse("2026-11-01T09:00:00Z");
        FieldTrip tripA = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 5L);
        FieldTrip tripB = activeTrip("Chuyến B", SITE_A, tripBStart, tripBEnd, 3L);
        StudyGroup groupA = group(tripA, "Lop A", ParticipationMode.INDIVIDUAL, 30);
        StudyGroup groupB = group(tripB, "Lop B", ParticipationMode.INDIVIDUAL, 30);
        UUID absentStudent = UUID.randomUUID();

        stubTeacher(teacherId);
        stubTrips(tripA, tripB);
        stubGroups(tripA, List.of(groupA), tripB, List.of(groupB));
        stubMembers(groupA, List.of(student1, absentStudent), 2);
        stubMembers(groupB, List.of(student2), 1);
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_A))
                .thenReturn(Optional.of(Location.builder().id(locationAId).siteCode(SITE_A).name("Cu Chi").build()));
        when(profileRepository.findAllById(any())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return List.of(
                            profile(student1, "An", "secret.student@example.com", "https://photos.example/secret-photo-1.jpg"),
                            profile(student2, "Binh", "secret.student2@example.com", "https://photos.example/secret-photo-2.jpg"),
                            profile(absentStudent, "Vang", "secret.absent@example.com", "https://photos.example/secret-photo-absent.jpg"))
                    .stream()
                    .filter(profile -> ids.contains(profile.getId()))
                    .toList();
        });
        stubScopedCheckins();
        stubScopedQuestProgress();
        when(snapshotRepository.save(any())).thenAnswer(invocation -> {
            FieldTripReportSnapshot snapshot = invocation.getArgument(0);
            snapshots.put(snapshot.getFieldTripId(), snapshot);
            return snapshot;
        });
        when(snapshotRepository.findByFieldTripId(any()))
                .thenAnswer(invocation -> Optional.ofNullable(snapshots.get(invocation.getArgument(0))));
        stubComplete(tripA, 5L);
        stubComplete(tripB, 3L);

        checkins.add(checkin(student1, locationAId, "ST01", 5, overlap));
        checkins.add(checkin(student2, locationAId, "ST09", 100, overlap.plusSeconds(600)));
        checkins.add(checkin(absentStudent, locationAId, "ST08", 40, TRIP_A_START.minusSeconds(60)));
        progress.add(questProgress(student1, "completed", overlap.plusSeconds(60)));
        progress.add(questProgress(student2, "completed", overlap.plusSeconds(900)));
        progress.add(questProgress(absentStudent, "completed", overlap.plusSeconds(120)));

        FieldTripLiveBoardResponse boardA = service.liveBoard(teacherId, tripA.getId());
        FieldTripLiveBoardResponse boardB = service.liveBoard(teacherId, tripB.getId());

        assertThat(boardA.rows()).extracting(Row::groupId).containsOnly(groupA.getId());
        assertThat(boardA.rows()).extracting(Row::userId).containsExactlyInAnyOrder(student1, absentStudent);
        assertThat(rowForUser(boardA, student1).stationsCompleted()).isEqualTo(1);
        assertThat(rowForUser(boardA, student1).points()).isEqualTo(5);
        assertThat(rowForUser(boardA, absentStudent).stationsCompleted()).isZero();
        assertThat(rowForUser(boardA, absentStudent).points()).isZero();
        assertThat(boardA.rows()).extracting(Row::points).doesNotContain(100);
        assertThat(boardB.rows()).extracting(Row::groupId).containsExactly(groupB.getId());
        assertThat(boardB.rows()).extracting(Row::userId).containsExactly(student2);
        assertThat(boardB.rows().get(0).stationsCompleted()).isEqualTo(1);
        assertThat(boardB.rows().get(0).points()).isEqualTo(100);

        FieldTripReportResponse reportA = service.completeAndSnapshot(teacherId, tripA.getId(), 5L);
        FieldTripReportResponse reportB = service.completeAndSnapshot(teacherId, tripB.getId(), 3L);

        assertThat(reportA.groups()).extracting(Group::groupId).containsExactly(groupA.getId());
        assertThat(reportB.groups()).extracting(Group::groupId).containsExactly(groupB.getId());
        Group metricsA = groupReport(reportA, groupA.getId());
        assertThat(metricsA.headcount()).isEqualTo(2);
        assertThat(metricsA.stationsCompleted()).isEqualTo(1);
        assertThat(metricsA.points()).isEqualTo(5);
        assertThat(metricsA.questCompletions()).isEqualTo(1);
        Group metricsB = groupReport(reportB, groupB.getId());
        assertThat(metricsB.headcount()).isEqualTo(1);
        assertThat(metricsB.stationsCompleted()).isEqualTo(1);
        assertThat(metricsB.points()).isEqualTo(100);
        assertThat(metricsB.questCompletions()).isEqualTo(1);
    }

    @Test
    void completeAndSnapshotReturnsTheSnapshotLifecycleStoredAndDoesNotWriteAnother() {
        FieldTrip trip = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 5L);
        FieldTripReportResponse stored = new FieldTripReportResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getSiteCode(),
                trip.getQuestId(),
                trip.getCampaignId(),
                FieldTripStatus.COMPLETED,
                Instant.parse("2026-11-01T06:00:00Z"),
                List.of());
        String storedJson = """
                {"tripId":"%s","title":"Chuyến A","siteCode":"cu-chi","questId":"%s","campaignId":"%s","status":"COMPLETED","generatedAt":"2026-11-01T06:00:00Z","groups":[]}
                """.formatted(trip.getId(), trip.getQuestId(), trip.getCampaignId()).replace("\n", "");
        stubTeacher(teacherId);
        stubTrips(trip);
        when(lifecycleService.complete(teacherId, trip.getId(), 5L)).thenAnswer(invocation -> {
            trip.setStatus(FieldTripStatus.COMPLETED);
            trip.setRowVersion(6L);
            snapshots.put(trip.getId(), FieldTripReportSnapshot.builder()
                    .fieldTripId(trip.getId())
                    .generatedAt(stored.generatedAt())
                    .reportJson(storedJson)
                    .build());
            return null;
        });
        when(snapshotRepository.findByFieldTripId(trip.getId()))
                .thenAnswer(invocation -> Optional.ofNullable(snapshots.get(trip.getId())));

        FieldTripReportResponse report = service.completeAndSnapshot(teacherId, trip.getId(), 5L);

        assertThat(report).isEqualTo(stored);
        verify(snapshotRepository, never()).save(any());
        verify(lifecycleService, times(1)).complete(teacherId, trip.getId(), 5L);
    }

    @Test
    void completeAndSnapshotHealsACompletedTripThatHasNoSnapshot() {
        FieldTrip trip = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 6L);
        trip.setStatus(FieldTripStatus.COMPLETED);
        stubTeacher(teacherId);
        stubTrips(trip);
        when(snapshotRepository.findByFieldTripId(trip.getId())).thenReturn(Optional.empty());

        FieldTripReportResponse report = service.completeAndSnapshot(teacherId, trip.getId(), 6L);

        assertThat(report.tripId()).isEqualTo(trip.getId());
        assertThat(report.status()).isEqualTo(FieldTripStatus.COMPLETED);
        assertThat(report.groups()).isEmpty();
        verify(lifecycleService, never()).complete(any(), any(), anyLong());
        verify(snapshotRepository).save(any());
    }

    @Test
    void sameOrgTeacherCanViewButCannotComplete() {
        FieldTrip trip = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 5L);
        UUID colleagueId = UUID.randomUUID();
        stubTeacher(teacherId);
        stubTeacher(colleagueId);
        stubTrips(trip);
        when(studyGroupRepository.findByFieldTripId(trip.getId())).thenReturn(List.of());
        stubSnapshotStore();
        stubComplete(trip, 5L);

        FieldTripLiveBoardResponse colleagueBoard = service.liveBoard(colleagueId, trip.getId());
        assertThat(colleagueBoard.rows()).isEmpty();
        assertThatThrownBy(() -> service.report(colleagueId, trip.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Báo cáo chỉ có sau khi kết thúc chuyến");

        service.completeAndSnapshot(teacherId, trip.getId(), 5L);

        FieldTripReportResponse colleagueReport = service.report(colleagueId, trip.getId());
        assertThat(colleagueReport.tripId()).isEqualTo(trip.getId());
        assertThat(colleagueReport.groups()).isEmpty();
        assertThat(colleagueReport.status()).isEqualTo(FieldTripStatus.COMPLETED);

        assertThatThrownBy(() -> service.completeAndSnapshot(colleagueId, trip.getId(), 6L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("người tạo");
        verify(lifecycleService, times(1)).complete(eq(teacherId), eq(trip.getId()), eq(5L));
        verify(lifecycleService, never()).complete(eq(colleagueId), any(), anyLong());
    }

    @Test
    void liveBoardAndReportRejectAnUnknownTrip() {
        UUID missing = UUID.randomUUID();
        when(fieldTripRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.liveBoard(teacherId, missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy chuyến đi");
        assertThatThrownBy(() -> service.report(teacherId, missing))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy chuyến đi");
    }

    @Test
    void reportRejectsACompletedTripThatHasNoSnapshot() {
        FieldTrip trip = activeTrip("Chuyến A", SITE_A, TRIP_A_START, TRIP_A_END, 5L);
        trip.setStatus(FieldTripStatus.COMPLETED);
        stubTeacher(teacherId);
        when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        when(snapshotRepository.findByFieldTripId(trip.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.report(teacherId, trip.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy báo cáo chuyến đi");
    }

    private void assertNoPersonalData(String json) throws Exception {
        assertThat(json).doesNotContain("@");
        assertThat(json).doesNotContain("secret");
        assertThat(json).doesNotContain("photo");
        assertThat(fieldNames(objectMapper.readTree(json)))
                .doesNotContain("email", "photoUrl", "avatarUrl", "chat", "passwordHash");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        collectFieldNames(node, names);
        return names;
    }

    private static void collectFieldNames(JsonNode node, List<String> names) {
        if (node == null || node.isNull() || node.isValueNode()) {
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFieldNames(child, names));
            return;
        }
        node.fieldNames().forEachRemaining(name -> {
            names.add(name);
            collectFieldNames(node.get(name), names);
        });
    }

    private static Row rowForUser(FieldTripLiveBoardResponse board, UUID userId) {
        return board.rows().stream()
                .filter(row -> userId.equals(row.userId()))
                .findFirst()
                .orElseThrow();
    }

    private static Group groupReport(FieldTripReportResponse report, UUID groupId) {
        return report.groups().stream()
                .filter(group -> groupId.equals(group.groupId()))
                .findFirst()
                .orElseThrow();
    }

    private void stubTeacher(UUID userId) {
        when(lmsAssignmentService.requirePremiumTeacher(userId))
                .thenReturn(Profile.builder().id(userId).orgId(orgId).displayName("Giao vien").build());
        when(organizationRepository.findById(orgId))
                .thenReturn(Optional.of(Organization.builder()
                        .id(orgId)
                        .status(BillingService.ORG_STATUS_ACTIVE)
                        .planType("PREMIUM")
                        .build()));
    }

    private void stubTrips(FieldTrip... trips) {
        for (FieldTrip trip : trips) {
            when(fieldTripRepository.findById(trip.getId())).thenReturn(Optional.of(trip));
        }
    }

    private void stubGroups(FieldTrip tripA, List<StudyGroup> groupsA, FieldTrip tripB, List<StudyGroup> groupsB) {
        when(studyGroupRepository.findByFieldTripId(tripA.getId())).thenReturn(groupsA);
        when(studyGroupRepository.findByFieldTripId(tripB.getId())).thenReturn(groupsB);
    }

    private void stubMembers(StudyGroup group, List<UUID> userIds, long count) {
        List<StudyGroupMember> members = userIds.stream()
                .map(userId -> StudyGroupMember.builder()
                        .id(UUID.randomUUID())
                        .groupId(group.getId())
                        .userId(userId)
                        .joinedAt(TRIP_A_START)
                        .build())
                .toList();
        when(studyGroupMemberRepository.findByGroupId(group.getId())).thenReturn(members);
        when(studyGroupMemberRepository.countByGroupId(group.getId())).thenReturn(count);
    }

    private void stubLocations() {
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_A))
                .thenReturn(Optional.of(Location.builder().id(locationAId).siteCode(SITE_A).name("Cu Chi").build()));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_B))
                .thenReturn(Optional.of(Location.builder().id(locationBId).siteCode(SITE_B).name("Hoi An").build()));
    }

    private void stubProfiles() {
        List<Profile> profiles = List.of(
                profile(student1, "An", "secret.student@example.com", "https://photos.example/secret-photo-1.jpg"),
                profile(student2, "Binh", "secret.student2@example.com", "https://photos.example/secret-photo-2.jpg"),
                profile(device1, "Device One", "secret.device@example.com", "https://photos.example/secret-photo-device.jpg"),
                profile(device2, "Device Two", "secret.device2@example.com", "https://photos.example/secret-photo-device-2.jpg"));
        when(profileRepository.findAllById(any())).thenAnswer(invocation -> {
            Collection<UUID> ids = invocation.getArgument(0);
            return profiles.stream().filter(profile -> ids.contains(profile.getId())).toList();
        });
    }

    private void stubScopedCheckins() {
        when(checkinRepository.findByUserIdInAndLocationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
                        any(), any(), any(), any()))
                .thenAnswer(invocation -> {
                    Collection<UUID> userIds = invocation.getArgument(0);
                    UUID locationId = invocation.getArgument(1);
                    Instant from = invocation.getArgument(2);
                    Instant to = invocation.getArgument(3);
                    return checkins.stream()
                            .filter(checkin -> userIds.contains(checkin.getUserId()))
                            .filter(checkin -> locationId.equals(checkin.getLocationId()))
                            .filter(checkin -> checkin.getCreatedAt() != null
                                    && !checkin.getCreatedAt().isBefore(from)
                                    && !checkin.getCreatedAt().isAfter(to))
                            .toList();
                });
    }

    private void stubScopedQuestProgress() {
        when(userQuestProgressRepository
                        .findByUserIdInAndQuestIdAndStatusIgnoreCaseAndCompletedAtGreaterThanEqualAndCompletedAtLessThanEqual(
                                any(), eq(questId), eq("completed"), any(), any()))
                .thenAnswer(invocation -> {
                    Collection<UUID> userIds = invocation.getArgument(0);
                    Instant from = invocation.getArgument(3);
                    Instant to = invocation.getArgument(4);
                    return progress.stream()
                            .filter(row -> userIds.contains(row.getUserId()))
                            .filter(row -> questId.equals(row.getQuestId()))
                            .filter(row -> row.getStatus() != null && row.getStatus().equalsIgnoreCase("completed"))
                            .filter(row -> row.getCompletedAt() != null
                                    && !row.getCompletedAt().isBefore(from)
                                    && !row.getCompletedAt().isAfter(to))
                            .toList();
                });
    }

    private void stubSnapshotStore() {
        when(snapshotRepository.save(any())).thenAnswer(invocation -> {
            FieldTripReportSnapshot snapshot = invocation.getArgument(0);
            snapshots.put(snapshot.getFieldTripId(), snapshot);
            return snapshot;
        });
        when(snapshotRepository.findByFieldTripId(any()))
                .thenAnswer(invocation -> Optional.ofNullable(snapshots.get(invocation.getArgument(0))));
    }

    private void stubComplete(FieldTrip trip, long expectedVersion) {
        when(lifecycleService.complete(teacherId, trip.getId(), expectedVersion)).thenAnswer(invocation -> {
            trip.setStatus(FieldTripStatus.COMPLETED);
            trip.setRowVersion(expectedVersion + 1);
            if (!snapshots.containsKey(trip.getId())) {
                FieldTripReportResponse report = reportAssembler.assemble(trip, Instant.now());
                snapshotRepository.save(reportAssembler.toSnapshot(trip.getId(), report));
            }
            return null;
        });
    }

    private FieldTrip activeTrip(String title, String siteCode, Instant start, Instant end, long version) {
        return FieldTrip.builder()
                .id(UUID.randomUUID())
                .orgId(orgId)
                .siteCode(siteCode)
                .title(title)
                .visitStart(start)
                .visitEnd(end)
                .questId(questId)
                .campaignId(campaignId)
                .joinCode("JOINCODE")
                .status(FieldTripStatus.ACTIVE)
                .siteConfirmationStatus(SiteConfirmationStatus.NOT_REQUIRED)
                .createdBy(teacherId)
                .createdAt(start)
                .updatedAt(start)
                .rowVersion(version)
                .build();
    }

    private StudyGroup group(FieldTrip trip, String name, ParticipationMode mode, int declaredSize) {
        return StudyGroup.builder()
                .id(UUID.randomUUID())
                .name(name)
                .code("ABCDEF")
                .createdBy(teacherId)
                .createdAt(trip.getVisitStart())
                .expiresAt(trip.getVisitEnd())
                .orgId(orgId)
                .questId(questId)
                .fieldTripId(trip.getId())
                .participationMode(mode)
                .declaredSize(declaredSize)
                .build();
    }

    private static Profile profile(UUID id, String displayName, String email, String avatarUrl) {
        return Profile.builder()
                .id(id)
                .email(email)
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .build();
    }

    private static Checkin checkin(UUID userId, UUID locationId, String stationCode, int score, Instant at) {
        return Checkin.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .locationId(locationId)
                .stationCode(stationCode)
                .presenceScore(score)
                .createdAt(at)
                .build();
    }

    private UserQuestProgress questProgress(UUID userId, String status, Instant completedAt) {
        return UserQuestProgress.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .questId(questId)
                .status(status)
                .completedAt(completedAt)
                .build();
    }
}
