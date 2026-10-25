package com.histar.be.fieldtrip.service;

import com.histar.be.checkin.entity.Checkin;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.fieldtrip.dto.FieldTripLiveBoardResponse;
import com.histar.be.fieldtrip.dto.FieldTripLiveBoardResponse.Row;
import com.histar.be.fieldtrip.dto.FieldTripReportResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripReportSnapshot;
import com.histar.be.fieldtrip.entity.FieldTripStatus;
import com.histar.be.fieldtrip.repository.FieldTripReportSnapshotRepository;
import com.histar.be.fieldtrip.repository.FieldTripRepository;
import com.histar.be.fieldtrip.service.FieldTripReportAssembler.GroupStats;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FieldTripReportingService {

    private static final String REPORT_NOT_READY = "Báo cáo chỉ có sau khi kết thúc chuyến";
    private static final String REPORT_MISSING = "Không tìm thấy báo cáo chuyến đi";
    private static final String TRIP_MISSING = "Không tìm thấy chuyến đi";
    private static final String COMPLETE_ONLY_ACTIVE = "Chỉ chuyến đang diễn ra mới được kết thúc";

    private final FieldTripRepository fieldTripRepository;
    private final FieldTripReportSnapshotRepository snapshotRepository;
    private final FieldTripLifecycleService lifecycleService;
    private final FieldTripAccessService fieldTripAccessService;
    private final StudyGroupRepository studyGroupRepository;
    private final ProfileRepository profileRepository;
    private final FieldTripReportAssembler reportAssembler;

    public FieldTripReportingService(
            FieldTripRepository fieldTripRepository,
            FieldTripReportSnapshotRepository snapshotRepository,
            FieldTripLifecycleService lifecycleService,
            FieldTripAccessService fieldTripAccessService,
            StudyGroupRepository studyGroupRepository,
            ProfileRepository profileRepository,
            FieldTripReportAssembler reportAssembler) {
        this.fieldTripRepository = fieldTripRepository;
        this.snapshotRepository = snapshotRepository;
        this.lifecycleService = lifecycleService;
        this.fieldTripAccessService = fieldTripAccessService;
        this.studyGroupRepository = studyGroupRepository;
        this.profileRepository = profileRepository;
        this.reportAssembler = reportAssembler;
    }

    @Transactional(readOnly = true)
    public FieldTripLiveBoardResponse liveBoard(UUID teacherId, UUID tripId) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanView(teacherId, trip);
        List<StudyGroup> groups = studyGroupRepository.findByFieldTripId(trip.getId());
        if (groups == null || groups.isEmpty()) {
            return new FieldTripLiveBoardResponse(trip.getId(), trip.getStatus(), List.of());
        }
        List<Row> rows = new ArrayList<>();
        for (StudyGroup group : groups) {
            GroupStats stats = reportAssembler.stats(trip, group);
            if (group.getParticipationMode() == ParticipationMode.SHARED_DEVICE) {
                rows.add(new Row(
                        group.getId(),
                        group.getName(),
                        ParticipationMode.SHARED_DEVICE,
                        null,
                        group.getName(),
                        stats.headcount(),
                        stats.stationsCompleted(),
                        stats.points()));
                continue;
            }
            Map<UUID, Profile> profiles = profiles(stats.userIds());
            ParticipationMode mode = group.getParticipationMode() == null
                    ? ParticipationMode.INDIVIDUAL
                    : group.getParticipationMode();
            for (UUID userId : stats.userIds()) {
                rows.add(new Row(
                        group.getId(),
                        group.getName(),
                        mode,
                        userId,
                        displayName(profiles.get(userId)),
                        1,
                        distinctStations(stats.checkins(), userId),
                        pointsOf(stats.checkins(), userId)));
            }
        }
        return new FieldTripLiveBoardResponse(trip.getId(), trip.getStatus(), List.copyOf(rows));
    }

    @Transactional(readOnly = true)
    public FieldTripReportResponse report(UUID teacherId, UUID tripId) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanView(teacherId, trip);
        if (trip.getStatus() != FieldTripStatus.COMPLETED) {
            throw new BusinessRuleException(REPORT_NOT_READY);
        }
        return readStored(tripId);
    }

    @Transactional
    public FieldTripReportResponse completeAndSnapshot(UUID teacherId, UUID tripId, long expectedVersion) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanEdit(teacherId, trip);
        if (trip.getStatus() == FieldTripStatus.COMPLETED) {
            return snapshotRepository
                    .findByFieldTripId(tripId)
                    .map(snapshot -> reportAssembler.read(snapshot.getReportJson()))
                    .orElseGet(() -> heal(trip));
        }
        if (trip.getStatus() != FieldTripStatus.ACTIVE) {
            throw new BusinessRuleException(COMPLETE_ONLY_ACTIVE);
        }
        lifecycleService.complete(teacherId, tripId, expectedVersion);
        return readStored(tripId);
    }

    private FieldTripReportResponse heal(FieldTrip trip) {
        FieldTripReportResponse report = reportAssembler.assemble(trip, Instant.now());
        snapshotRepository.save(reportAssembler.toSnapshot(trip.getId(), report));
        return report;
    }

    private Map<UUID, Profile> profiles(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(userIds).forEach(profile -> profiles.put(profile.getId(), profile));
        return profiles;
    }

    private FieldTripReportResponse readStored(UUID tripId) {
        FieldTripReportSnapshot snapshot = snapshotRepository
                .findByFieldTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(REPORT_MISSING));
        return reportAssembler.read(snapshot.getReportJson());
    }

    private FieldTrip load(UUID tripId) {
        return fieldTripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_MISSING));
    }

    private static String displayName(Profile profile) {
        if (profile == null || profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
            return "Học sinh";
        }
        return profile.getDisplayName();
    }

    private static int distinctStations(List<Checkin> checkins, UUID userId) {
        Set<String> stations = new LinkedHashSet<>();
        for (Checkin checkin : checkins) {
            if (userId != null && !userId.equals(checkin.getUserId())) {
                continue;
            }
            if (hasText(checkin.getStationCode())) {
                stations.add(checkin.getStationCode());
            }
        }
        return stations.size();
    }

    private static int pointsOf(List<Checkin> checkins, UUID userId) {
        int points = 0;
        for (Checkin checkin : checkins) {
            if (userId != null && !userId.equals(checkin.getUserId())) {
                continue;
            }
            if (checkin.getPresenceScore() != null) {
                points += checkin.getPresenceScore();
            }
        }
        return points;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
