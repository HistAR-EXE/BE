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
import com.histar.be.fieldtrip.dto.FieldTripReportResponse;
import com.histar.be.fieldtrip.entity.FieldTrip;
import com.histar.be.fieldtrip.entity.FieldTripReportSnapshot;
import com.histar.be.group.entity.ParticipationMode;
import com.histar.be.group.entity.StudyGroup;
import com.histar.be.group.entity.StudyGroupMember;
import com.histar.be.group.repository.StudyGroupMemberRepository;
import com.histar.be.group.repository.StudyGroupRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FieldTripReportAssembler {

    private final StudyGroupRepository studyGroupRepository;
    private final StudyGroupMemberRepository studyGroupMemberRepository;
    private final CheckinRepository checkinRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final LocationRepository locationRepository;
    private final ObjectMapper objectMapper;

    public FieldTripReportAssembler(
            StudyGroupRepository studyGroupRepository,
            StudyGroupMemberRepository studyGroupMemberRepository,
            CheckinRepository checkinRepository,
            UserQuestProgressRepository userQuestProgressRepository,
            LocationRepository locationRepository,
            ObjectMapper objectMapper) {
        this.studyGroupRepository = studyGroupRepository;
        this.studyGroupMemberRepository = studyGroupMemberRepository;
        this.checkinRepository = checkinRepository;
        this.userQuestProgressRepository = userQuestProgressRepository;
        this.locationRepository = locationRepository;
        this.objectMapper = snapshotMapper(objectMapper);
    }

    public FieldTripReportResponse assemble(FieldTrip trip, Instant generatedAt) {
        List<StudyGroup> groups = studyGroupRepository.findByFieldTripId(trip.getId());
        List<FieldTripReportResponse.Group> rows = new ArrayList<>();
        if (groups != null) {
            for (StudyGroup group : groups) {
                GroupStats stats = stats(trip, group);
                rows.add(new FieldTripReportResponse.Group(
                        group.getId(),
                        group.getName(),
                        group.getParticipationMode(),
                        stats.headcount(),
                        stats.stationsCompleted(),
                        stats.questCompletions(),
                        stats.points()));
            }
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

    public FieldTripReportSnapshot toSnapshot(UUID fieldTripId, FieldTripReportResponse report) {
        return FieldTripReportSnapshot.builder()
                .fieldTripId(fieldTripId)
                .generatedAt(report.generatedAt())
                .reportJson(write(report))
                .build();
    }

    public FieldTripReportResponse read(String reportJson) {
        try {
            return objectMapper.readValue(reportJson, FieldTripReportResponse.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể đọc báo cáo chuyến đi", ex);
        }
    }

    public GroupStats stats(FieldTrip trip, StudyGroup group) {
        List<UUID> userIds = memberIds(group.getId());
        long memberCount = studyGroupMemberRepository.countByGroupId(group.getId());
        int headcount = group.getParticipationMode() == ParticipationMode.SHARED_DEVICE
                ? declaredSize(group)
                : (int) memberCount;
        List<Checkin> checkins = checkinsFor(userIds, locationFor(trip), trip.getVisitStart(), trip.getVisitEnd());
        return new GroupStats(
                userIds,
                headcount,
                distinctStations(checkins, null),
                pointsOf(checkins, null),
                questCompletions(userIds, trip, checkins),
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

    private int questCompletions(List<UUID> userIds, FieldTrip trip, List<Checkin> checkins) {
        if (userIds.isEmpty() || trip.getQuestId() == null || trip.getVisitStart() == null || trip.getVisitEnd() == null) {
            return 0;
        }
        Set<UUID> present = new LinkedHashSet<>();
        for (Checkin checkin : checkins) {
            if (checkin.getUserId() != null) {
                present.add(checkin.getUserId());
            }
        }
        if (present.isEmpty()) {
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
                .filter(present::contains)
                .distinct()
                .count();
    }

    private Location locationFor(FieldTrip trip) {
        if (trip.getSiteCode() == null || trip.getSiteCode().isBlank()) {
            return null;
        }
        return locationRepository.findBySiteCodeIgnoreCase(trip.getSiteCode()).orElse(null);
    }

    private String write(FieldTripReportResponse report) {
        try {
            return objectMapper.writeValueAsString(report);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể lưu báo cáo chuyến đi", ex);
        }
    }

    private static int declaredSize(StudyGroup group) {
        Integer declared = group.getDeclaredSize();
        if (declared == null || declared < 0) {
            return 0;
        }
        return declared;
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

    public record GroupStats(
            List<UUID> userIds,
            int headcount,
            int stationsCompleted,
            int points,
            int questCompletions,
            List<Checkin> checkins) {}
}
