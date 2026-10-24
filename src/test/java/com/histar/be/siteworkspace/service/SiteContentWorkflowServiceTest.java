package com.histar.be.siteworkspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.checkin.presence.StationQrService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.exception.NarrativeConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.siteworkspace.dto.ContentReviewDecision;
import com.histar.be.siteworkspace.dto.ContentReviewRequest;
import com.histar.be.siteworkspace.dto.SiteContentDraftRequest;
import com.histar.be.siteworkspace.dto.SiteContentVersionResponse;
import com.histar.be.siteworkspace.dto.SiteStationQrResponse;
import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.entity.SiteContentKind;
import com.histar.be.siteworkspace.entity.SiteContentVersion;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.repository.SiteContentVersionRepository;
import com.histar.be.stations.dto.StationContentSnapshot;
import com.histar.be.stations.entity.Station;
import com.histar.be.stations.entity.StationBlockType;
import com.histar.be.stations.repository.StationRepository;
import com.histar.be.stations.service.StationService;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class SiteContentWorkflowServiceTest {

    private static final String SITE = "imperial-city";
    private static final String PROFILE_KEY = "profile";
    private static final String STATION_CODE = "ST01";
    private static final String MEDIA = "https://cdn.example/clip.mp4";
    private static final Set<SiteRole> EDITORS = Set.of(SiteRole.SITE_MANAGER, SiteRole.CONTENT_EDITOR);
    private static final Set<SiteRole> REVIEWERS = Set.of(SiteRole.CONTENT_REVIEWER, SiteRole.SITE_MANAGER);
    private static final Set<SiteRole> PUBLISHERS = Set.of(SiteRole.SITE_MANAGER);

    @Mock
    private SiteContentVersionRepository versions;

    @Mock
    private LocationRepository locations;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private StationService stationService;

    @Mock
    private SiteWorkspaceAccessService access;

    @Mock
    private AdminAuditService audit;

    @Mock
    private StationQrService stationQrService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private SiteContentWorkflowService service;
    private Location location;
    private UUID locationId;
    private UUID questId;
    private UUID editorId;
    private UUID reviewerId;
    private UUID managerId;

    @BeforeEach
    void setUp() {
        service = new SiteContentWorkflowService(
                versions,
                locations,
                stationRepository,
                stationService,
                access,
                audit,
                stationQrService,
                objectMapper);
        locationId = UUID.randomUUID();
        questId = UUID.randomUUID();
        editorId = UUID.randomUUID();
        reviewerId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        location = Location.builder()
                .id(locationId)
                .siteCode(SITE)
                .name("Imperial")
                .description("old")
                .formattedAddress("old address")
                .coverImage("old-cover")
                .sources("old-sources")
                .latitude(10.5)
                .longitude(106.5)
                .rating(4.2)
                .isArAvailable(Boolean.TRUE)
                .unlockPrerequisiteQuestId(questId)
                .unlockNarrative("keep-me")
                .city("Hue")
                .googleMapsUrl("https://maps.example/old")
                .knowledgeContext("secret-context")
                .build();
        lenient().when(locations.findBySiteCodeIgnoreCase(anyString())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return SITE.equalsIgnoreCase(code) ? Optional.of(location) : Optional.empty();
        });
        lenient().when(versions.saveAndFlush(any(SiteContentVersion.class))).thenAnswer(invocation -> {
            SiteContentVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId(UUID.randomUUID());
            }
            return version;
        });
    }

    @Test
    void doesNotAddWorkspaceRolesToPlatformUserRole() {
        assertThat(UserRole.values())
                .containsExactly(UserRole.USER, UserRole.ORG_MEMBER, UserRole.ADMIN, UserRole.TEACHER);
    }

    @Test
    void requestContractsExposeRevisionAndHideSigningSecret() {
        assertThat(recordNames(SiteContentDraftRequest.class)).containsExactly("snapshot", "expectedRevision");
        assertThat(recordNames(ContentReviewRequest.class)).containsExactly("decision", "note", "expectedRevision");
        assertThat(ContentReviewDecision.values())
                .containsExactly(ContentReviewDecision.APPROVE, ContentReviewDecision.REQUEST_CHANGES);
        assertThat(recordNames(SiteStationQrResponse.class))
                .contains("payload", "signature")
                .doesNotContain("qrHmacSecret", "secret", "hmacSecret");
    }

    @Test
    void mutationsAreTransactionalSoAFailedPublishRollsBack() throws Exception {
        for (String name : List.of(
                "createDraft", "updateDraft", "submitForReview", "review", "publish", "rollback")) {
            assertThat(Arrays.stream(SiteContentWorkflowService.class.getDeclaredMethods())
                            .filter(method -> method.getName().equals(name))
                            .findFirst()
                            .orElseThrow()
                            .getAnnotations())
                    .extracting(annotation -> annotation.annotationType().getSimpleName())
                    .contains("Transactional");
        }
        assertThat(StationService.class.getMethod("applyPublishedSnapshot", String.class, StationContentSnapshot.class)
                        .isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class))
                .isTrue();
    }

    @Test
    void createDraft_profileStoresSiteCodeAndLocationFk() throws Exception {
        stubNoInFlight(SiteContentKind.SITE_PROFILE, SITE);

        SiteContentVersionResponse response =
                service.createDraft(editorId, "IMPERIAL-CITY", SiteContentKind.SITE_PROFILE, PROFILE_KEY, draft(profileJson()));

        assertThat(response.resourceKey()).isEqualTo(PROFILE_KEY);
        assertThat(response.version()).isEqualTo(1);
        assertThat(response.status()).isEqualTo(ContentLifecycleStatus.DRAFT);
        SiteContentVersion saved = captureSaved();
        assertThat(saved.getResourceKey()).isEqualTo(SITE);
        assertThat(saved.getLocationId()).isEqualTo(locationId);
        assertThat(saved.getStationId()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo(editorId);
        assertThat(saved.getSnapshotJson()).contains("New name").doesNotContain("billing", "unlockNarrative");
        verify(access).requireAny(editorId, SITE, EDITORS);
    }

    @Test
    void createDraft_rejectsReviewerAndProtectedProfileFields() throws Exception {
        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(access)
                .requireAny(reviewerId, SITE, EDITORS);
        assertThatThrownBy(() -> service.createDraft(
                        reviewerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, draft(profileJson())))
                .isInstanceOf(BusinessRuleException.class);
        verify(versions, never()).saveAndFlush(any());

        for (String key : List.of(
                "id",
                "siteCode",
                "unlockPrerequisiteQuestId",
                "unlockNarrative",
                "rating",
                "billing",
                "lat",
                "lng",
                "latitude",
                "longitude",
                "isArAvailable")) {
            assertThatThrownBy(() -> service.createDraft(
                            editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, draft(withExtra(profileJson(), key))))
                    .as(key)
                    .isInstanceOf(BusinessRuleException.class);
        }
        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.SITE_PROFILE, SITE, draft(profileJson())))
                .isInstanceOf(BusinessRuleException.class);
        verify(versions, never()).saveAndFlush(any());
    }

    @Test
    void createDraft_stationBindsStationFkAndRejectsInvalidOrMalformedBlocks() throws Exception {
        Station station = stubStation(true);
        stubNoInFlight(SiteContentKind.STATION, STATION_CODE);

        SiteContentVersionResponse created = service.createDraft(
                managerId, SITE, SiteContentKind.STATION, "st01", draft(stationJson("TEXT", null)));

        SiteContentVersion saved = captureSaved();
        assertThat(created.resourceKey()).isEqualTo(STATION_CODE);
        assertThat(saved.getResourceKey()).isEqualTo(STATION_CODE);
        assertThat(saved.getStationId()).isEqualTo(station.getId());
        assertThat(saved.getLocationId()).isNull();
        assertThat(saved.getSnapshotJson()).contains("\"lat\"").contains("\"blocks\"");

        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.STATION, STATION_CODE, draft(stationJson("PODCAST", MEDIA))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.STATION, STATION_CODE, draft(stationJson("IMAGE", "not a url"))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.STATION, STATION_CODE, draft(stationJson("AUDIO", "javascript:alert(1)"))))
                .isInstanceOf(BusinessRuleException.class);

        when(stationRepository.findBySiteCodeAndCode(SITE, "ST99")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.STATION, "ST99", draft(stationJson("TEXT", null).replace("ST01", "ST99"))))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy trạm");
        verify(stationRepository, never()).findFirstByCodeIgnoreCaseAndActiveTrueOrderBySortOrderAsc(any());
        verify(versions, org.mockito.Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void createDraft_rejectsSecondInFlightVersion() throws Exception {
        SiteContentVersion existing = content(SiteContentKind.SITE_PROFILE, SITE, 1, ContentLifecycleStatus.IN_REVIEW, profileJson(), 2);
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.SITE_PROFILE, SITE, ContentLifecycleStatus.DRAFT))
                .thenReturn(List.of());
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.SITE_PROFILE, SITE, ContentLifecycleStatus.IN_REVIEW))
                .thenReturn(List.of(existing));
        assertThatThrownBy(() -> service.createDraft(
                        editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, draft(profileJson())))
                .isInstanceOf(BusinessRuleException.class);
        verify(versions, never()).saveAndFlush(any());
    }

    @Test
    void updateDraft_replacesSnapshotAndRejectsStaleRevisionOrNonDraft() throws Exception {
        SiteContentVersion version = content(SiteContentKind.SITE_PROFILE, SITE, 3, ContentLifecycleStatus.DRAFT, profileJson(), 4);
        stubLookup(version);

        SiteContentVersionResponse updated = service.updateDraft(
                editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 3, 4, draft(profileJson().replace("New name", "Edited")));

        assertThat(updated.status()).isEqualTo(ContentLifecycleStatus.DRAFT);
        assertThat(version.getSnapshotJson()).contains("Edited");
        verify(access).requireAny(editorId, SITE, EDITORS);

        assertConflict(() -> service.updateDraft(
                editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 3, 3, draft(profileJson())));

        version.setStatus(ContentLifecycleStatus.IN_REVIEW);
        version.setRevision(4);
        assertThatThrownBy(() -> service.updateDraft(
                        editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 3, 4, draft(profileJson())))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void optimisticLockFailureWhileSavingMapsToVersionConflict() throws Exception {
        SiteContentVersion version = content(SiteContentKind.SITE_PROFILE, SITE, 3, ContentLifecycleStatus.DRAFT, profileJson(), 4);
        stubLookup(version);
        when(versions.saveAndFlush(any(SiteContentVersion.class)))
                .thenThrow(new OptimisticLockingFailureException("stale row"));

        assertConflict(() -> service.updateDraft(
                editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 3, 4, draft(profileJson())));
        assertThat(version.getSnapshotJson()).contains("New name");
    }

    @Test
    void submitAndReviewFollowTheLifecycleAndAuditWithoutMedia() throws Exception {
        SiteContentVersion version = content(
                SiteContentKind.STATION, STATION_CODE, 2, ContentLifecycleStatus.DRAFT, stationJson("VIDEO", MEDIA), 1);
        version.setStationId(UUID.randomUUID());
        stubLookup(version);

        SiteContentVersionResponse submitted =
                service.submitForReview(editorId, SITE, SiteContentKind.STATION, STATION_CODE, 2, 1);

        assertThat(submitted.status()).isEqualTo(ContentLifecycleStatus.IN_REVIEW);
        assertThat(version.getSubmittedBy()).isEqualTo(editorId);
        assertAudit("SITE_CONTENT_SUBMITTED", MEDIA);

        version.setRevision(1);
        SiteContentVersionResponse approved = service.review(
                reviewerId,
                SITE,
                SiteContentKind.STATION,
                STATION_CODE,
                2,
                1,
                ContentReviewDecision.APPROVE,
                "ok");
        assertThat(approved.status()).isEqualTo(ContentLifecycleStatus.APPROVED);
        assertThat(version.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(version.getReviewNote()).isEqualTo("ok");
        verify(access).requireAny(reviewerId, SITE, REVIEWERS);

        version.setStatus(ContentLifecycleStatus.IN_REVIEW);
        version.setRevision(1);
        version.setReviewedBy(null);
        SiteContentVersionResponse returned = service.review(
                managerId,
                SITE,
                SiteContentKind.STATION,
                STATION_CODE,
                2,
                1,
                ContentReviewDecision.REQUEST_CHANGES,
                "add the caption");
        assertThat(returned.status()).isEqualTo(ContentLifecycleStatus.DRAFT);
        assertThat(version.getReviewNote()).isEqualTo("add the caption");
        verify(stationService, never()).applyPublishedSnapshot(anyString(), any());
    }

    @Test
    void reviewRejectsSelfReviewEvenWhenTheActorCouldOtherwiseReview() throws Exception {
        SiteContentVersion version = content(SiteContentKind.SITE_PROFILE, SITE, 1, ContentLifecycleStatus.IN_REVIEW, profileJson(), 2);
        version.setSubmittedBy(managerId);
        stubLookup(version);

        assertThatThrownBy(() -> service.review(
                        managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 2, ContentReviewDecision.APPROVE, "self"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(version.getStatus()).isEqualTo(ContentLifecycleStatus.IN_REVIEW);
        verify(versions, never()).saveAndFlush(any());
    }

    @Test
    void twoReviewersOnTheSameRevision_secondGetsVersionConflict() throws Exception {
        SiteContentVersion version = content(SiteContentKind.SITE_PROFILE, SITE, 4, ContentLifecycleStatus.IN_REVIEW, profileJson(), 7);
        version.setSubmittedBy(editorId);
        stubLookup(version);
        when(versions.saveAndFlush(any(SiteContentVersion.class))).thenAnswer(invocation -> {
            SiteContentVersion saved = invocation.getArgument(0);
            saved.setRevision(saved.getRevision() + 1);
            return saved;
        });

        service.review(reviewerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 4, 7, ContentReviewDecision.APPROVE, "first");

        assertThat(version.getStatus()).isEqualTo(ContentLifecycleStatus.APPROVED);
        assertConflict(() -> service.review(
                managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 4, 7, ContentReviewDecision.APPROVE, "second"));
        assertThat(version.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(version.getReviewNote()).isEqualTo("first");
    }

    @Test
    void approveAndPublishRejectBlankMediaForPlaybackBlocks() throws Exception {
        for (String type : List.of("IMAGE", "AUDIO", "VIDEO")) {
            for (String media : List.of("", "   ")) {
                SiteContentVersion version = content(
                        SiteContentKind.STATION, STATION_CODE, 1, ContentLifecycleStatus.IN_REVIEW, stationJson(type, media), 1);
                version.setSubmittedBy(editorId);
                stubLookup(version);
                assertThatThrownBy(() -> service.review(
                                reviewerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, 1, ContentReviewDecision.APPROVE, "no"))
                        .as(type + " " + media)
                        .isInstanceOf(BusinessRuleException.class);
                version.setStatus(ContentLifecycleStatus.APPROVED);
                assertThatThrownBy(() -> service.publish(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, 1))
                        .isInstanceOf(BusinessRuleException.class);
                verify(stationService, never()).applyPublishedSnapshot(anyString(), any());
            }
        }
    }

    @Test
    void publishProfileArchivesPreviousAndLeavesProtectedLocationFieldsUntouched() throws Exception {
        SiteContentVersion previous = content(SiteContentKind.SITE_PROFILE, SITE, 1, ContentLifecycleStatus.PUBLISHED, "{\"name\":\"old\"}", 2);
        String previousJson = previous.getSnapshotJson();
        SiteContentVersion current = content(SiteContentKind.SITE_PROFILE, SITE, 2, ContentLifecycleStatus.APPROVED, profileJson(), 5);
        current.setLocationId(locationId);
        stubLookup(current);
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.SITE_PROFILE, SITE, ContentLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(previous));

        SiteContentVersionResponse published =
                service.publish(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 2, 5);

        assertThat(published.status()).isEqualTo(ContentLifecycleStatus.PUBLISHED);
        assertThat(previous.getStatus()).isEqualTo(ContentLifecycleStatus.ARCHIVED);
        assertThat(previous.getSnapshotJson()).isEqualTo(previousJson);
        assertThat(location.getName()).isEqualTo("New name");
        assertThat(location.getDescription()).isEqualTo("New description");
        assertThat(location.getFormattedAddress()).isEqualTo("New address");
        assertThat(location.getCoverImage()).isEqualTo("https://cdn.example/cover.jpg");
        assertThat(location.getSources()).isEqualTo("New sources");
        assertThat(location.getId()).isEqualTo(locationId);
        assertThat(location.getSiteCode()).isEqualTo(SITE);
        assertThat(location.getLatitude()).isEqualTo(10.5);
        assertThat(location.getLongitude()).isEqualTo(106.5);
        assertThat(location.getRating()).isEqualTo(4.2);
        assertThat(location.getIsArAvailable()).isTrue();
        assertThat(location.getUnlockPrerequisiteQuestId()).isEqualTo(questId);
        assertThat(location.getUnlockNarrative()).isEqualTo("keep-me");
        assertThat(location.getCity()).isEqualTo("Hue");
        assertThat(location.getGoogleMapsUrl()).isEqualTo("https://maps.example/old");
        assertThat(location.getKnowledgeContext()).isEqualTo("secret-context");
        verify(access).requireAny(managerId, SITE, PUBLISHERS);
        verify(stationService, never()).importStations(any(), any());
        verify(locations).save(location);
        assertAudit("SITE_CONTENT_PUBLISHED", "https://cdn.example/cover.jpg");
    }

    @Test
    void publishStationAppliesSnapshotAndArchivesPreviousWithoutImportBypass() throws Exception {
        stubStation(true);
        SiteContentVersion previous = content(
                SiteContentKind.STATION, STATION_CODE, 1, ContentLifecycleStatus.PUBLISHED, stationJson("TEXT", null), 1);
        String previousJson = previous.getSnapshotJson();
        SiteContentVersion current = content(
                SiteContentKind.STATION, STATION_CODE, 2, ContentLifecycleStatus.APPROVED, stationJson("VIDEO", MEDIA), 3);
        stubLookup(current);
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.STATION, STATION_CODE, ContentLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(previous));

        service.publish(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 2, 3);

        ArgumentCaptor<StationContentSnapshot> snapshot = ArgumentCaptor.forClass(StationContentSnapshot.class);
        verify(stationService).applyPublishedSnapshot(eq(SITE), snapshot.capture());
        assertThat(snapshot.getValue().stationCode()).isEqualTo(STATION_CODE);
        assertThat(snapshot.getValue().name()).isEqualTo("Ben moi");
        assertThat(snapshot.getValue().blocks()).singleElement().satisfies(block -> {
            assertThat(block.blockType()).isEqualTo(StationBlockType.VIDEO);
            assertThat(block.mediaUrl()).isEqualTo(MEDIA);
        });
        verify(stationService, never()).importStations(any(), any());
        assertThat(previous.getStatus()).isEqualTo(ContentLifecycleStatus.ARCHIVED);
        assertThat(previous.getSnapshotJson()).isEqualTo(previousJson);
        assertThat(current.getStatus()).isEqualTo(ContentLifecycleStatus.PUBLISHED);
        assertAudit("SITE_CONTENT_PUBLISHED", MEDIA);
    }

    @Test
    void publishRollsBackWhenSnapshotApplyThrows() throws Exception {
        SiteContentVersion current = content(
                SiteContentKind.STATION, STATION_CODE, 2, ContentLifecycleStatus.APPROVED, stationJson("VIDEO", MEDIA), 3);
        stubLookup(current);
        doThrow(new IllegalStateException("snapshot apply failed"))
                .when(stationService)
                .applyPublishedSnapshot(anyString(), any());

        assertThatThrownBy(() -> service.publish(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 2, 3))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("snapshot apply failed");
        assertThat(current.getStatus()).isEqualTo(ContentLifecycleStatus.APPROVED);
        verify(versions, never()).saveAndFlush(any());
        verify(audit, never()).append(any());
    }

    @Test
    void publishProfileSaveFailureDoesNotMarkVersionPublished() throws Exception {
        SiteContentVersion current = content(SiteContentKind.SITE_PROFILE, SITE, 2, ContentLifecycleStatus.APPROVED, profileJson(), 5);
        stubLookup(current);
        when(locations.save(any(Location.class))).thenThrow(new IllegalStateException("location write failed"));

        assertThatThrownBy(() -> service.publish(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 2, 5))
                .isInstanceOf(IllegalStateException.class);
        assertThat(current.getStatus()).isEqualTo(ContentLifecycleStatus.APPROVED);
        verify(versions, never()).saveAndFlush(any());
    }

    @Test
    void publishRejectsStaleRevisionAndNonApprovedStatus() throws Exception {
        SiteContentVersion current = content(SiteContentKind.SITE_PROFILE, SITE, 2, ContentLifecycleStatus.APPROVED, profileJson(), 5);
        stubLookup(current);
        assertConflict(() -> service.publish(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 2, 4));
        verify(locations, never()).save(any());

        for (ContentLifecycleStatus status : List.of(
                ContentLifecycleStatus.DRAFT,
                ContentLifecycleStatus.IN_REVIEW,
                ContentLifecycleStatus.PUBLISHED,
                ContentLifecycleStatus.ARCHIVED)) {
            current.setStatus(status);
            current.setRevision(5);
            assertThatThrownBy(() -> service.publish(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 2, 5))
                    .as(status.name())
                    .isInstanceOf(BusinessRuleException.class);
        }
        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(access)
                .requireAny(editorId, SITE, PUBLISHERS);
        current.setStatus(ContentLifecycleStatus.APPROVED);
        assertThatThrownBy(() -> service.publish(editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 2, 5))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void illegalTransitionsStayOnTheirCurrentStatus() throws Exception {
        SiteContentVersion version = content(SiteContentKind.SITE_PROFILE, SITE, 1, ContentLifecycleStatus.DRAFT, profileJson(), 1);
        version.setSubmittedBy(editorId);
        stubLookup(version);

        assertThatThrownBy(() -> service.review(
                        reviewerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1, ContentReviewDecision.APPROVE, "x"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.rollback(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, "back", 1))
                .isInstanceOf(BusinessRuleException.class);

        version.setStatus(ContentLifecycleStatus.IN_REVIEW);
        assertThatThrownBy(() -> service.submitForReview(editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.updateDraft(editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1, draft(profileJson())))
                .isInstanceOf(BusinessRuleException.class);

        version.setStatus(ContentLifecycleStatus.APPROVED);
        assertThatThrownBy(() -> service.submitForReview(editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.review(
                        reviewerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1, ContentReviewDecision.APPROVE, "x"))
                .isInstanceOf(BusinessRuleException.class);

        version.setStatus(ContentLifecycleStatus.ARCHIVED);
        version.setPublishedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        assertThatThrownBy(() -> service.submitForReview(editorId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.review(
                        reviewerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, 1, ContentReviewDecision.REQUEST_CHANGES, "x"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(version.getSnapshotJson()).contains("New name");
    }

    @Test
    void rollbackRestoresArchivedSnapshotWithoutMutatingItsJson() throws Exception {
        String oldJson = profileJson().replace("New name", "Restored");
        SiteContentVersion archived = content(SiteContentKind.SITE_PROFILE, SITE, 1, ContentLifecycleStatus.ARCHIVED, oldJson, 2);
        archived.setPublishedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        archived.setReviewNote("original review");
        SiteContentVersion current = content(SiteContentKind.SITE_PROFILE, SITE, 2, ContentLifecycleStatus.PUBLISHED, profileJson(), 9);
        String currentJson = current.getSnapshotJson();
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndVersion(
                        SITE, SiteContentKind.SITE_PROFILE, SITE, 1))
                .thenReturn(Optional.of(archived));
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.SITE_PROFILE, SITE, ContentLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(current));
        when(versions.findFirstBySiteCodeIgnoreCaseAndContentKindAndResourceKeyOrderByVersionDesc(
                        SITE, SiteContentKind.SITE_PROFILE, SITE))
                .thenReturn(Optional.of(current));

        SiteContentVersionResponse restored =
                service.rollback(managerId, SITE, SiteContentKind.SITE_PROFILE, PROFILE_KEY, 1, "restore the plaque", 9);

        assertThat(archived.getStatus()).isEqualTo(ContentLifecycleStatus.ARCHIVED);
        assertThat(archived.getSnapshotJson()).isEqualTo(oldJson);
        assertThat(archived.getReviewNote()).isEqualTo("original review");
        assertThat(current.getStatus()).isEqualTo(ContentLifecycleStatus.ARCHIVED);
        assertThat(current.getSnapshotJson()).isEqualTo(currentJson);
        assertThat(location.getName()).isEqualTo("Restored");
        assertThat(location.getLatitude()).isEqualTo(10.5);
        assertThat(location.getUnlockNarrative()).isEqualTo("keep-me");
        ArgumentCaptor<SiteContentVersion> saved = ArgumentCaptor.forClass(SiteContentVersion.class);
        verify(versions, org.mockito.Mockito.atLeastOnce()).saveAndFlush(saved.capture());
        assertThat(saved.getAllValues()).anySatisfy(version -> {
            assertThat(version).isNotSameAs(archived);
            assertThat(version.getStatus()).isEqualTo(ContentLifecycleStatus.PUBLISHED);
            assertThat(version.getSnapshotJson()).isEqualTo(oldJson);
            assertThat(version.getVersion()).isEqualTo(3);
            assertThat(version.getPublishedBy()).isEqualTo(managerId);
        });
        assertThat(restored.version()).isEqualTo(3);
        assertThat(restored.status()).isEqualTo(ContentLifecycleStatus.PUBLISHED);
        verify(access).requireAny(managerId, SITE, PUBLISHERS);
        assertAudit("SITE_CONTENT_ROLLED_BACK", "https://cdn.example/cover.jpg");
    }

    @Test
    void rollbackDoesNotPublishWhenRuntimeApplyFails() throws Exception {
        String oldJson = stationJson("TEXT", null);
        SiteContentVersion archived = content(SiteContentKind.STATION, STATION_CODE, 1, ContentLifecycleStatus.ARCHIVED, oldJson, 2);
        archived.setPublishedAt(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        SiteContentVersion current = content(
                SiteContentKind.STATION, STATION_CODE, 2, ContentLifecycleStatus.PUBLISHED, stationJson("VIDEO", MEDIA), 4);
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndVersion(
                        SITE, SiteContentKind.STATION, STATION_CODE, 1))
                .thenReturn(Optional.of(archived));
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                        SITE, SiteContentKind.STATION, STATION_CODE, ContentLifecycleStatus.PUBLISHED))
                .thenReturn(List.of(current));
        doThrow(new IllegalStateException("snapshot apply failed"))
                .when(stationService)
                .applyPublishedSnapshot(anyString(), any());

        assertThatThrownBy(() -> service.rollback(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, "back", 4))
                .isInstanceOf(IllegalStateException.class);
        assertThat(current.getStatus()).isEqualTo(ContentLifecycleStatus.PUBLISHED);
        assertThat(archived.getSnapshotJson()).isEqualTo(oldJson);
        verify(versions, never()).saveAndFlush(argThat(version -> version.getStatus() == ContentLifecycleStatus.PUBLISHED));
    }

    @Test
    void issueStaticQrRequiresActiveSameSiteEditorAndHidesTheSecret() {
        Station station = stubStation(true);
        when(stationQrService.issueStatic(SITE, station.getCode()))
                .thenReturn(new StationQrService.SignedStationQr(
                        SITE, STATION_CODE, 0L, "sig", SITE + ":" + STATION_CODE + ":0:sig", true, true));

        SiteStationQrResponse response = service.issueStaticQr(editorId, SITE, "st01");

        assertThat(response.payload()).isEqualTo(SITE + ":" + STATION_CODE + ":0:sig");
        assertThat(response.signature()).isEqualTo("sig");
        assertThat(response.staticQr()).isTrue();
        assertThat(response.toString()).doesNotContain("qrHmacSecret");
        verify(access).requireAny(editorId, SITE, EDITORS);

        station.setActive(false);
        assertThatThrownBy(() -> service.issueStaticQr(editorId, SITE, STATION_CODE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy trạm");

        when(stationRepository.findBySiteCodeAndCode(eq(SITE), eq("ST99"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.issueStaticQr(managerId, SITE, "ST99"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy trạm");
        verify(stationRepository, never()).findFirstByCodeIgnoreCaseAndActiveTrueOrderBySortOrderAsc(any());

        doThrow(new BusinessRuleException("Không có quyền truy cập site workspace này"))
                .when(access)
                .requireAny(reviewerId, SITE, EDITORS);
        assertThatThrownBy(() -> service.issueStaticQr(reviewerId, SITE, STATION_CODE))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void approveAndPublishRejectSnapshotWithoutActive() {
        assertIncompleteStationRejected(
                "{\"stationCode\":\"ST01\",\"name\":\"Ben moi\",\"sortOrder\":2,\"lat\":11.1,\"lng\":107.2,"
                        + "\"questStepKey\":\"step-1\",\"blocks\":[]}");
    }

    @Test
    void approveAndPublishRejectSnapshotWithoutBlocks() {
        assertIncompleteStationRejected(
                "{\"stationCode\":\"ST01\",\"name\":\"Ben moi\",\"sortOrder\":2,\"lat\":11.1,\"lng\":107.2,"
                        + "\"questStepKey\":\"step-1\",\"active\":true}");
    }

    @Test
    void approveAndPublishRejectSnapshotWithOmittedKeys() {
        assertIncompleteStationRejected("{\"stationCode\":\"ST01\",\"name\":\"Ben moi\"}");
        assertIncompleteStationRejected(
                "{\"stationCode\":\"ST01\",\"name\":\"Ben moi\",\"sortOrder\":2,\"questStepKey\":\"step-1\","
                        + "\"active\":true,\"blocks\":[]}");
    }

    @Test
    void approveAndPublishAcceptsCompleteStationSnapshot() {
        StationContentSnapshot applied = approveAndPublish(stationJson("TEXT", null));

        assertThat(applied.active()).isTrue();
        assertThat(applied.name()).isEqualTo("Ben moi");
        assertThat(applied.sortOrder()).isEqualTo(2);
        assertThat(applied.lat()).isEqualTo(11.1);
        assertThat(applied.lng()).isEqualTo(107.2);
        assertThat(applied.blocks()).singleElement().satisfies(block ->
                assertThat(block.blockType()).isEqualTo(StationBlockType.TEXT));
    }

    @Test
    void approveAndPublishAcceptsExplicitEmptyBlocksAndActiveTrue() {
        StationContentSnapshot applied = approveAndPublish(
                "{\"stationCode\":\"ST01\",\"name\":\"Ben moi\",\"sortOrder\":2,\"lat\":11.1,\"lng\":107.2,"
                        + "\"questStepKey\":\"step-1\",\"active\":true,\"blocks\":[]}");

        assertThat(applied.active()).isTrue();
        assertThat(applied.blocks()).isEmpty();
    }

    @Test
    void createDraft_uniqueKeyViolationMapsToVersionConflict() throws Exception {
        stubStation(true);
        stubNoInFlight(SiteContentKind.STATION, STATION_CODE);
        when(versions.saveAndFlush(any(SiteContentVersion.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate key value violates unique constraint \"uq_site_content_versions_resource_version\""));

        assertConflict(() -> service.createDraft(
                editorId, SITE, SiteContentKind.STATION, STATION_CODE, draft(stationJson("TEXT", null))));
    }

    @Test
    void listVersions_requiresReaderRoleAndPassesOptionalStatus() throws Exception {
        when(versions.findBySiteCodeIgnoreCaseOrderByUpdatedAtDescVersionDesc(SITE)).thenReturn(null);

        assertThat(service.listVersions(editorId, "IMPERIAL-CITY", null)).isEmpty();
        verify(access).requireAny(
                editorId,
                SITE,
                Set.of(SiteRole.SITE_MANAGER, SiteRole.CONTENT_EDITOR, SiteRole.CONTENT_REVIEWER));
        verify(versions).findBySiteCodeIgnoreCaseOrderByUpdatedAtDescVersionDesc(SITE);
        verify(versions, never())
                .findBySiteCodeIgnoreCaseAndStatusOrderByUpdatedAtDescVersionDesc(anyString(), any());

        SiteContentVersion inReview = content(
                SiteContentKind.SITE_PROFILE, SITE, 2, ContentLifecycleStatus.IN_REVIEW, profileJson(), 4);
        when(versions.findBySiteCodeIgnoreCaseAndStatusOrderByUpdatedAtDescVersionDesc(
                        SITE, ContentLifecycleStatus.IN_REVIEW))
                .thenReturn(List.of(inReview));

        List<SiteContentVersionResponse> listed =
                service.listVersions(reviewerId, SITE, ContentLifecycleStatus.IN_REVIEW);

        assertThat(listed).singleElement().satisfies(item -> {
            assertThat(item.resourceKey()).isEqualTo(PROFILE_KEY);
            assertThat(item.status()).isEqualTo(ContentLifecycleStatus.IN_REVIEW);
            assertThat(item.version()).isEqualTo(2);
            assertThat(item.revision()).isEqualTo(4);
        });
        verify(access).requireAny(
                reviewerId,
                SITE,
                Set.of(SiteRole.SITE_MANAGER, SiteRole.CONTENT_EDITOR, SiteRole.CONTENT_REVIEWER));
    }

    private void assertIncompleteStationRejected(String json) {
        SiteContentVersion version = content(
                SiteContentKind.STATION, STATION_CODE, 1, ContentLifecycleStatus.IN_REVIEW, json, 1);
        version.setSubmittedBy(editorId);
        stubLookup(version);

        assertThatThrownBy(() -> service.review(
                        reviewerId,
                        SITE,
                        SiteContentKind.STATION,
                        STATION_CODE,
                        1,
                        1,
                        ContentReviewDecision.APPROVE,
                        "no"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(version.getStatus()).isEqualTo(ContentLifecycleStatus.IN_REVIEW);

        version.setStatus(ContentLifecycleStatus.APPROVED);
        assertThatThrownBy(() -> service.publish(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, 1))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(version.getStatus()).isEqualTo(ContentLifecycleStatus.APPROVED);
        verify(stationService, never()).applyPublishedSnapshot(anyString(), any());
    }

    private StationContentSnapshot approveAndPublish(String json) {
        SiteContentVersion version = content(
                SiteContentKind.STATION, STATION_CODE, 1, ContentLifecycleStatus.IN_REVIEW, json, 1);
        version.setSubmittedBy(editorId);
        stubLookup(version);

        SiteContentVersionResponse approved = service.review(
                reviewerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, 1, ContentReviewDecision.APPROVE, "ok");
        assertThat(approved.status()).isEqualTo(ContentLifecycleStatus.APPROVED);

        SiteContentVersionResponse published =
                service.publish(managerId, SITE, SiteContentKind.STATION, STATION_CODE, 1, 1);
        assertThat(published.status()).isEqualTo(ContentLifecycleStatus.PUBLISHED);

        ArgumentCaptor<StationContentSnapshot> snapshot = ArgumentCaptor.forClass(StationContentSnapshot.class);
        verify(stationService).applyPublishedSnapshot(eq(SITE), snapshot.capture());
        return snapshot.getValue();
    }

    private void stubNoInFlight(SiteContentKind kind, String persistedKey) {
        for (ContentLifecycleStatus status : List.of(
                ContentLifecycleStatus.DRAFT, ContentLifecycleStatus.IN_REVIEW, ContentLifecycleStatus.APPROVED)) {
            when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndStatusOrderByVersionDesc(
                            SITE, kind, persistedKey, status))
                    .thenReturn(List.of());
        }
        when(versions.findFirstBySiteCodeIgnoreCaseAndContentKindAndResourceKeyOrderByVersionDesc(SITE, kind, persistedKey))
                .thenReturn(Optional.empty());
    }

    private void stubLookup(SiteContentVersion version) {
        when(versions.findBySiteCodeIgnoreCaseAndContentKindAndResourceKeyAndVersion(
                        SITE, version.getContentKind(), version.getResourceKey(), version.getVersion()))
                .thenReturn(Optional.of(version));
    }

    private Station stubStation(boolean active) {
        Station station = Station.builder()
                .id(UUID.randomUUID())
                .siteCode(SITE)
                .code(STATION_CODE)
                .name("Old station")
                .active(active)
                .build();
        lenient().when(stationRepository.findBySiteCodeAndCode(SITE, STATION_CODE)).thenReturn(Optional.of(station));
        lenient().when(stationRepository.findBySiteCodeAndCode(SITE, "st01")).thenReturn(Optional.empty());
        lenient().when(stationRepository.findBySiteCodeAndCode(SITE, "ST01")).thenReturn(Optional.of(station));
        return station;
    }

    private SiteContentVersion captureSaved() {
        ArgumentCaptor<SiteContentVersion> captor = ArgumentCaptor.forClass(SiteContentVersion.class);
        verify(versions).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private void assertAudit(String action, String forbiddenMedia) {
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit, org.mockito.Mockito.atLeastOnce()).append(captor.capture());
        AdminAuditCommand command = captor.getAllValues().stream()
                .filter(item -> action.equals(item.action()))
                .findFirst()
                .orElseThrow();
        assertThat(command.entityType()).isEqualTo("SITE_CONTENT");
        assertThat(command.metadata()).doesNotContainKeys("snapshot", "snapshotJson", "mediaUrl", "blocks", "qrHmacSecret");
        assertThat(command.metadata().values()).allSatisfy(value -> {
            assertThat(value).isNotInstanceOf(byte[].class);
            assertThat(String.valueOf(value)).doesNotContain(forbiddenMedia);
        });
        if (command.reason() != null) {
            assertThat(command.reason()).doesNotContain(forbiddenMedia);
        }
    }

    private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable executable) {
        assertThatThrownBy(executable)
                .isInstanceOf(NarrativeConflictException.class)
                .extracting(ex -> ((NarrativeConflictException) ex).getErrorCode())
                .isEqualTo(ErrorCode.VERSION_CONFLICT);
    }

    private SiteContentVersion content(
            SiteContentKind kind, String resourceKey, int version, ContentLifecycleStatus status, String json, long revision) {
        return SiteContentVersion.builder()
                .id(UUID.randomUUID())
                .siteCode(SITE)
                .contentKind(kind)
                .resourceKey(resourceKey)
                .version(version)
                .status(status)
                .snapshotJson(json)
                .revision(revision)
                .createdBy(editorId)
                .build();
    }

    private SiteContentDraftRequest draft(String json) throws Exception {
        return new SiteContentDraftRequest(objectMapper.readTree(json), null);
    }

    private String profileJson() {
        return "{"
                + "\"name\":\"New name\","
                + "\"description\":\"New description\","
                + "\"formattedAddress\":\"New address\","
                + "\"coverImage\":\"https://cdn.example/cover.jpg\","
                + "\"sources\":\"New sources\""
                + "}";
    }

    private String withExtra(String json, String key) {
        return json.substring(0, json.lastIndexOf('}')) + ",\"" + key + "\":\"no\"}";
    }

    private String stationJson(String blockType, String mediaUrl) {
        String media = mediaUrl == null ? "null" : "\"" + mediaUrl + "\"";
        return "{"
                + "\"stationCode\":\"ST01\","
                + "\"name\":\"Ben moi\","
                + "\"sortOrder\":2,"
                + "\"lat\":11.1,"
                + "\"lng\":107.2,"
                + "\"questStepKey\":\"step-1\","
                + "\"active\":true,"
                + "\"blocks\":[{\"blockType\":\""
                + blockType
                + "\",\"title\":\"t\",\"body\":\"b\",\"mediaUrl\":"
                + media
                + ",\"sortOrder\":1,\"metaJson\":null}]"
                + "}";
    }

    private static List<String> recordNames(Class<?> type) {
        return Arrays.stream(type.getRecordComponents()).map(RecordComponent::getName).toList();
    }

}
