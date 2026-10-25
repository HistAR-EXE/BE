package com.histar.be.siteworkspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.discovery.entity.UserDiscovery;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.events.entity.PilotEvent;
import com.histar.be.events.repository.PilotEventRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.entity.UserQuestProgress;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.siteworkspace.controller.SiteOperationsController;
import com.histar.be.siteworkspace.dto.SiteOperationsSummaryResponse;
import com.histar.be.siteworkspace.entity.SiteMembership;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.entity.SiteWorkspaceSettings;
import com.histar.be.siteworkspace.repository.SiteMembershipRepository;
import com.histar.be.siteworkspace.repository.SiteWorkspaceSettingsRepository;
import com.histar.be.visit.entity.VisitSession;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SiteOperationsAnalyticsServiceTest {

    private static final Instant FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-10-08T00:00:00Z");
    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "userId",
            "user_id",
            "email",
            "displayName",
            "photoUrl",
            "photo",
            "chat",
            "score",
            "presenceScore");

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Autowired
    private SiteOperationsAnalyticsService service;

    @Autowired
    private SiteOperationsController controller;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private SiteMembershipRepository siteMembershipRepository;

    @Autowired
    private SiteWorkspaceSettingsRepository siteWorkspaceSettingsRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VisitSessionRepository visitSessionRepository;

    @Autowired
    private CheckinRepository checkinRepository;

    @Autowired
    private UserQuestProgressRepository userQuestProgressRepository;

    @Autowired
    private UserDiscoveryRepository userDiscoveryRepository;

    @Autowired
    private PilotEventRepository pilotEventRepository;

    private String siteCode;
    private UUID locationId;
    private Profile manager;
    private Profile reviewer;
    private Profile editor;

    @BeforeEach
    void setUp() {
        siteCode = "ops-" + UUID.randomUUID().toString().substring(0, 8);
        locationId = locationRepository.save(Location.builder()
                        .siteCode(siteCode)
                        .name("Operations Site")
                        .build())
                .getId();
        manager = saveProfile("manager");
        reviewer = saveProfile("reviewer");
        editor = saveProfile("editor");
        grant(siteCode, manager.getId(), SiteRole.SITE_MANAGER);
        grant(siteCode, reviewer.getId(), SiteRole.CONTENT_REVIEWER);
        grant(siteCode, editor.getId(), SiteRole.CONTENT_EDITOR);
        siteWorkspaceSettingsRepository.save(SiteWorkspaceSettings.builder()
                .siteCode(siteCode)
                .requiresTripConfirmation(true)
                .updatedBy(manager.getId())
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void summary_oneUserOneCheckin_serializesAggregatesOnly() throws Exception {
        Profile visitor = saveProfile("visitor-secret@example.com", "Secret Visitor", "https://cdn.example/secret-photo.jpg");
        checkinRepository.save(Checkin.builder()
                .userId(visitor.getId())
                .locationId(locationId)
                .stationCode("ngo-mon")
                .presenceScore(987654)
                .latitude(21.123456)
                .longitude(105.987654)
                .createdAt(FROM.plusSeconds(60))
                .build());

        SiteOperationsSummaryResponse summary = service.summary(manager.getId(), siteCode, FROM, TO);
        JsonNode json = objectMapper.valueToTree(summary);
        String raw = objectMapper.writeValueAsString(summary);

        assertThat(fieldNames(json)).containsExactlyInAnyOrder(
                "siteCode",
                "from",
                "to",
                "visitCount",
                "checkinsByStation",
                "stationCode",
                "count",
                "questCompletions",
                "contentInteractions");
        assertThat(fieldNames(json)).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
        assertThat(json.get("visitCount").asLong()).isZero();
        assertThat(json.get("questCompletions").isNumber()).isTrue();
        assertThat(json.get("questCompletions").asLong()).isZero();
        assertThat(json.get("checkinsByStation")).hasSize(1);
        assertThat(json.get("checkinsByStation").get(0).propertyNames())
                .containsExactlyInAnyOrder("stationCode", "count");
        assertThat(json.get("checkinsByStation").get(0).get("stationCode").asText()).isEqualTo("ngo-mon");
        assertThat(json.get("checkinsByStation").get(0).get("count").asLong()).isEqualTo(1);
        assertThat(json.get("contentInteractions").isObject()).isTrue();
        assertThat(json.get("contentInteractions").isEmpty()).isTrue();
        assertPrivacyTextAbsent(raw, visitor);
        assertThat(raw).doesNotContain("987654", "21.123456", "requiresTripConfirmation", "signature");
    }

    @Test
    void summary_aggregatesSiteActivityAndDropsPersonalContent() throws Exception {
        Profile visitor = saveProfile("visitor-secret@example.com", "Secret Visitor", "https://cdn.example/secret-photo.jpg");
        Location other = locationRepository.save(Location.builder()
                .siteCode("other-" + UUID.randomUUID().toString().substring(0, 8))
                .name("Other Site")
                .build());

        VisitSession inRange = visitSessionRepository.save(session(visitor.getId(), locationId, FROM));
        visitSessionRepository.save(session(visitor.getId(), locationId, TO));
        VisitSession otherSession = visitSessionRepository.save(session(visitor.getId(), other.getId(), FROM.plusSeconds(10)));

        checkinRepository.save(checkin(visitor.getId(), locationId, "ngo-mon", FROM.plusSeconds(30)));
        checkinRepository.save(checkin(visitor.getId(), locationId, "ngo-mon", FROM.plusSeconds(90)));
        checkinRepository.save(checkin(visitor.getId(), locationId, "thai-hoa", FROM.plusSeconds(120)));
        checkinRepository.save(checkin(visitor.getId(), locationId, "thai-hoa", TO));
        checkinRepository.save(checkin(visitor.getId(), other.getId(), "ngo-mon", FROM.plusSeconds(30)));
        checkinRepository.save(Checkin.builder()
                .userId(visitor.getId())
                .locationId(locationId)
                .presenceScore(987654)
                .createdAt(FROM.plusSeconds(15))
                .build());

        userQuestProgressRepository.save(quest(visitor.getId(), locationId, "completed", FROM.plusSeconds(5)));
        userQuestProgressRepository.save(quest(visitor.getId(), locationId, "COMPLETED", FROM.plusSeconds(15)));
        userQuestProgressRepository.save(quest(visitor.getId(), locationId, "in_progress", FROM.plusSeconds(20)));
        userQuestProgressRepository.save(quest(visitor.getId(), locationId, "completed", TO));
        userQuestProgressRepository.save(quest(visitor.getId(), other.getId(), "completed", FROM.plusSeconds(5)));

        userDiscoveryRepository.save(discovery(visitor.getId(), locationId, "gate", FROM.plusSeconds(8)));
        userDiscoveryRepository.save(discovery(visitor.getId(), locationId, "late", TO));
        userDiscoveryRepository.save(discovery(visitor.getId(), other.getId(), "gate", FROM.plusSeconds(8)));

        pilotEventRepository.save(event("video_played", inRange.getId(), "played the courtyard film", FROM.plusSeconds(40)));
        pilotEventRepository.save(event("audio_played", inRange.getId(), "audio guide", FROM.plusSeconds(50)));
        pilotEventRepository.save(event(
                "chat_message", inRange.getId(), "private-chat-transcript visitor-secret@example.com", FROM.plusSeconds(55)));
        pilotEventRepository.save(event("nps_submitted", inRange.getId(), "score=10", FROM.plusSeconds(56)));
        pilotEventRepository.save(event("checkin_result", inRange.getId(), "presenceScore=987654", FROM.plusSeconds(57)));
        pilotEventRepository.save(event("video_played", otherSession.getId(), "other site film", FROM.plusSeconds(40)));
        pilotEventRepository.save(event("video_played", inRange.getId(), "outside window", TO));

        SiteOperationsSummaryResponse summary = service.summary(reviewer.getId(), siteCode.toUpperCase(), FROM, TO);
        JsonNode json = objectMapper.valueToTree(summary);
        String raw = objectMapper.writeValueAsString(ApiResponse.ok(summary));

        assertThat(summary.siteCode()).isEqualTo(siteCode);
        assertThat(summary.visitCount()).isEqualTo(1);
        assertThat(summary.questCompletions()).isEqualTo(2);
        assertThat(summary.checkinsByStation()).containsExactly(
                new SiteOperationsSummaryResponse.StationCheckinCount("ngo-mon", 2),
                new SiteOperationsSummaryResponse.StationCheckinCount("thai-hoa", 1));
        assertThat(summary.contentInteractions()).containsExactlyInAnyOrderEntriesOf(java.util.Map.of(
                "discovery", 1L,
                "video_played", 1L,
                "audio_played", 1L));
        assertThat(fieldNames(json)).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
        assertThat(json.get("questCompletions").isArray()).isFalse();
        assertPrivacyTextAbsent(raw, visitor);
        assertThat(raw).doesNotContain(
                "private-chat-transcript",
                "987654",
                "requiresTripConfirmation",
                "chat_message",
                "nps_submitted",
                "checkin_result",
                "score");
    }

    @Test
    void summary_rejectsEditorAndCrossSite() {
        Location other = locationRepository.save(Location.builder()
                .siteCode("foreign-" + UUID.randomUUID().toString().substring(0, 8))
                .name("Foreign Site")
                .build());
        grant(other.getSiteCode(), manager.getId(), SiteRole.SITE_MANAGER);

        assertThatThrownBy(() -> service.summary(editor.getId(), siteCode, FROM, TO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
        assertThatThrownBy(() -> service.summary(reviewer.getId(), other.getSiteCode(), FROM, TO))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không có quyền");
    }

    @Test
    void summary_adminBypassReadsRequestedSiteOnly() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "platform-admin",
                null,
                java.util.List.of(new SimpleGrantedAuthority(UserRole.ADMIN.authority()))));
        checkinRepository.save(checkin(manager.getId(), locationId, "ngo-mon", FROM.plusSeconds(1)));

        SiteOperationsSummaryResponse summary = service.summary(UUID.randomUUID(), siteCode, FROM, TO);

        assertThat(summary.visitCount()).isZero();
        assertThat(summary.checkinsByStation()).containsExactly(
                new SiteOperationsSummaryResponse.StationCheckinCount("ngo-mon", 1));
    }

    @Test
    void summary_usesHalfOpenRangeAndRejectsInvalidWindow() {
        visitSessionRepository.save(session(manager.getId(), locationId, FROM));
        visitSessionRepository.save(session(manager.getId(), locationId, FROM.minusSeconds(1)));
        visitSessionRepository.save(session(manager.getId(), locationId, TO));

        assertThat(service.summary(manager.getId(), siteCode, FROM, TO).visitCount()).isEqualTo(1);
        assertThatThrownBy(() -> service.summary(manager.getId(), siteCode, TO, FROM))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.summary(manager.getId(), siteCode, FROM, FROM))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.summary(manager.getId(), siteCode, null, TO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void operationsEndpoint_returnsSameAggregate() throws Exception {
        authenticate(manager);
        checkinRepository.save(checkin(manager.getId(), locationId, "ngo-mon", FROM.plusSeconds(3)));

        ApiResponse<SiteOperationsSummaryResponse> response = controller.operations(siteCode, FROM, TO);
        JsonNode json = objectMapper.valueToTree(response);

        assertThat(response.isSuccess()).isTrue();
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("success", "data");
        assertThat(json.get("data").get("checkinsByStation").get(0).get("count").asLong()).isEqualTo(1);
        assertThat(fieldNames(json)).doesNotContainAnyElementsOf(FORBIDDEN_FIELDS);
    }

    private void authenticate(Profile profile) {
        User principal = new User(profile.getEmail(), "password", java.util.List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private Profile saveProfile(String label) {
        return saveProfile(label + "-" + UUID.randomUUID() + "@test.local", label, null);
    }

    private Profile saveProfile(String email, String displayName, String avatarUrl) {
        return profileRepository.save(Profile.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName(displayName)
                .avatarUrl(avatarUrl)
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());
    }

    private void grant(String code, UUID userId, SiteRole role) {
        siteMembershipRepository.save(SiteMembership.builder()
                .siteCode(code)
                .userId(userId)
                .role(role)
                .active(true)
                .build());
    }

    private VisitSession session(UUID userId, UUID siteLocationId, Instant startedAt) {
        return VisitSession.builder()
                .userId(userId)
                .locationId(siteLocationId)
                .mode("offline")
                .status("ACTIVE")
                .startedAt(startedAt)
                .build();
    }

    private Checkin checkin(UUID userId, UUID siteLocationId, String stationCode, Instant createdAt) {
        return Checkin.builder()
                .userId(userId)
                .locationId(siteLocationId)
                .stationCode(stationCode)
                .presenceScore(987654)
                .createdAt(createdAt)
                .build();
    }

    private UserQuestProgress quest(UUID userId, UUID siteLocationId, String status, Instant completedAt) {
        return UserQuestProgress.builder()
                .userId(userId)
                .questId(UUID.randomUUID())
                .locationId(siteLocationId)
                .status(status)
                .currentStep(3)
                .stepsTotal(4)
                .startedAt(completedAt.minusSeconds(30))
                .completedAt("in_progress".equals(status) ? null : completedAt)
                .build();
    }

    private UserDiscovery discovery(UUID userId, UUID siteLocationId, String key, Instant discoveredAt) {
        return UserDiscovery.builder()
                .userId(userId)
                .locationId(siteLocationId)
                .discoveryKey(key)
                .discoveredAt(discoveredAt)
                .build();
    }

    private PilotEvent event(String eventType, UUID sessionId, String payload, Instant occurredAt) {
        return PilotEvent.builder()
                .userId(UUID.randomUUID())
                .sessionId(sessionId)
                .eventType(eventType)
                .payloadJson(payload)
                .occurredAt(occurredAt)
                .createdAt(occurredAt)
                .build();
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        collectFieldNames(node, names);
        return names;
    }

    private static void collectFieldNames(JsonNode node, Set<String> names) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            for (String name : node.propertyNames()) {
                names.add(name);
                collectFieldNames(node.get(name), names);
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectFieldNames(child, names));
        }
    }

    private static void assertPrivacyTextAbsent(String raw, Profile visitor) {
        assertThat(raw).doesNotContain(
                visitor.getId().toString(),
                visitor.getEmail(),
                visitor.getDisplayName(),
                visitor.getAvatarUrl());
    }
}
