package com.histar.be.fieldtrip.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
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
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final CheckinRepository checkinRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final LocationRepository locationRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public FieldTripReportingService(
            FieldTripRepository fieldTripRepository,
            FieldTripReportSnapshotRepository snapshotRepository,
            FieldTripLifecycleService lifecycleService,
            FieldTripAccessService fieldTripAccessService,
            StudyGroupRepository studyGroupRepository,
            StudyGroupMemberRepository studyGroupMemberRepository,
            CheckinRepository checkinRepository,
            UserQuestProgressRepository userQuestProgressRepository,
            LocationRepository locationRepository,
            ProfileRepository profileRepository,
            ObjectMapper objectMapper) {
        this.fieldTripRepository = fieldTripRepository;
        this.snapshotRepository = snapshotRepository;
        this.lifecycleService = lifecycleService;
        this.fieldTripAccessService = fieldTripAccessService;
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.checkinRepository = checkinRepository;
        this.userQuestProgressRepository = userQuestProgressRepository;
        this.locationRepository = locationRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = snapshotMapper(objectMapper);
    }

    @Transactional(readOnly = true)
    public FieldTripLiveBoardResponse liveBoard(UUID teacherId, UUID tripId) {
        FieldTrip trip = load(tripId);
        fieldTripAccessService.requireCanView(teacherId, trip);
        List<StudyGroup> groups = studyGroupRepository.findByFieldTripId(trip.getId());
        if (groups.isEmpty()) {
            return new FieldTripLiveBoardResponse(trip.getId(), trip.getStatus(), List.of());
        }
        Location location = locationFor(trip);
        List<Row> rows = new ArrayList<>();
        for (StudyGroup group : groups) {
            GroupStats stats = stats(trip, location, group);
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
            return readStored(tripId);
        }
        if (trip.getStatus() != FieldTripStatus.ACTIVE) {
            throw new BusinessRuleException(COMPLETE_ONLY_ACTIVE);
        }
        lifecycleService.complete(teacherId, tripId, expectedVersion);
        trip = load(tripId);
        FieldTripReportResponse report = buildReport(trip, Instant.now());
        snapshotRepository.save(FieldTripReportSnapshot.builder()
                .fieldTripId(trip.getId())
                .generatedAt(report.generatedAt())
                .reportJson(write(report))
                .build());
        return report;
    }

    private FieldTripReportResponse buildReport(FieldTrip trip, Instant generatedAt) {
        List<StudyGroup> groups = studyGroupRepository.findByFieldTripId(trip.getId());
        Location location = groups.isEmpty() ? null : locationFor(trip);
        List<FieldTripReportResponse.Group> rows = new ArrayList<>();
        for (StudyGroup group : groups) {
            GroupStats stats = stats(trip, location, group);
            rows.add(new FieldTripReportResponse.Group(
                    group.getId(),
                    group.getName(),
                    group.getParticipationMode(),
                    stats.headcount(),
                    stats.stationsCompleted(),
                    stats.questCompletions(),
                    stats.points()));
        }
        return new FieldTripReportResponse(
                trip.getId(),
                trip.getTitle(),
                trip.getSiteCode(),
                trip.getQuestId(),
                trip.getCampaignId(),
                trip.getStatus(),
                generatedAt,
                List.copyOf(rows));
    }

    private GroupStats stats(FieldTrip trip, Location location, StudyGroup group) {
        List<UUID> userIds = memberIds(group.getId());
        long memberCount = studyGroupMemberRepository.countByGroupId(group.getId());
        int headcount = group.getParticipationMode() == ParticipationMode.SHARED_DEVICE
                ? declaredSize(group)
                : (int) memberCount;
        List<Checkin> checkins = checkinsFor(userIds, location, trip.getVisitStart(), trip.getVisitEnd());
        return new GroupStats(
                userIds,
                headcount,
                distinctStations(checkins, null),
                pointsOf(checkins, null),
                questCompletions(userIds, trip),
                checkins);
    }

    private List<UUID> memberIds(UUID groupId) {
        return studyGroupMemberRepository.findByGroupId(groupId).stream()
                .map(StudyGroupMember::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<Checkin> checkinsFor(List<UUID> userIds, Location location, Instant from, Instant to) {
        if (userIds.isEmpty() || location == null || location.getId() == null || from == null || to == null) {
            return List.of();
        }
        List<Checkin> rows =
                checkinRepository.findByUserIdInAndLocationIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqual(
                        userIds, location.getId(), from, to);
        return rows == null ? List.of() : rows;
    }

    private int questCompletions(List<UUID> userIds, FieldTrip trip) {
        if (userIds.isEmpty() || trip.getQuestId() == null || trip.getVisitStart() == null || trip.getVisitEnd() == null) {
            return 0;
        }
        List<UserQuestProgress> rows = userQuestProgressRepository
                .findByUserIdInAndQuestIdAndStatusIgnoreCaseAndCompletedAtGreaterThanEqualAndCompletedAtLessThanEqual(
                        userIds, trip.getQuestId(), "completed", trip.getVisitStart(), trip.getVisitEnd());
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        return (int) rows.stream()
                .map(UserQuestProgress::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private Map<UUID, Profile> profiles(List<UUID> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Profile> profiles = new HashMap<>();
        profileRepository.findAllById(userIds).forEach(profile -> profiles.put(profile.getId(), profile));
        return profiles;
    }

    private Location locationFor(FieldTrip trip) {
        if (trip.getSiteCode() == null || trip.getSiteCode().isBlank()) {
            return null;
        }
        return locationRepository.findBySiteCodeIgnoreCase(trip.getSiteCode()).orElse(null);
    }

    private FieldTripReportResponse readStored(UUID tripId) {
        FieldTripReportSnapshot snapshot = snapshotRepository
                .findByFieldTripId(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(REPORT_MISSING));
        try {
            return objectMapper.readValue(snapshot.getReportJson(), FieldTripReportResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể đọc báo cáo chuyến đi", ex);
        }
    }

    private String write(FieldTripReportResponse report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu báo cáo chuyến đi", ex);
        }
    }

    private FieldTrip load(UUID tripId) {
        return fieldTripRepository
                .findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException(TRIP_MISSING));
    }

    private static int declaredSize(StudyGroup group) {
        Integer declared = group.getDeclaredSize();
        if (declared == null || declared < 0) {
            return 0;
        }
        return declared;
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

    private static ObjectMapper snapshotMapper(ObjectMapper source) {
        SimpleModule times = new SimpleModule();
        times.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(value.toString());
            }
        });
        times.addDeserializer(Instant.class, new JsonDeserializer<>() {
            @Override
            public Instant deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                String text = parser.getText();
                return text == null || text.isBlank() ? null : Instant.parse(text);
            }
        });
        return source.copy().registerModule(times).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private record GroupStats(
            List<UUID> userIds,
            int headcount,
            int stationsCompleted,
            int points,
            int questCompletions,
            List<Checkin> checkins) {}
}
