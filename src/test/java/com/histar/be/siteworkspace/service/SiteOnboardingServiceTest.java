package com.histar.be.siteworkspace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.siteworkspace.dto.CreateSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.ReviewSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.SiteOnboardingResponse;
import com.histar.be.siteworkspace.entity.SiteMembership;
import com.histar.be.siteworkspace.entity.SiteOnboardingRequest;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import com.histar.be.siteworkspace.entity.SiteRole;
import com.histar.be.siteworkspace.repository.SiteMembershipRepository;
import com.histar.be.siteworkspace.repository.SiteOnboardingRequestRepository;
import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class SiteOnboardingServiceTest {

    private static final String SITE_CODE = "imperial-city";
    private static final String EVIDENCE = "Giấy ủy quyền số 12, liên hệ văn phòng di tích";

    @Mock
    private SiteOnboardingRequestRepository siteOnboardingRequestRepository;

    @Mock
    private SiteMembershipRepository siteMembershipRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private SiteOnboardingService service;

    @Test
    void createRequestDoesNotAcceptActorFromBody() {
        assertThat(CreateSiteOnboardingRequest.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly(
                        "representativeName",
                        "organizationName",
                        "representativeEmail",
                        "siteCode",
                        "evidenceNote")
                .doesNotContain("userId", "requesterId", "actorId");
        assertThat(ReviewSiteOnboardingRequest.class.getRecordComponents())
                .extracting(RecordComponent::getName)
                .containsExactly("note");
    }

    @Test
    void submit_persistsPendingRequestForCallerAndNormalizedSite() {
        UUID actorId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.of(knownSite()));
        when(siteOnboardingRequestRepository.existsByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
                        actorId, SITE_CODE, SiteOnboardingStatus.PENDING))
                .thenReturn(false);
        when(siteOnboardingRequestRepository.save(any(SiteOnboardingRequest.class))).thenAnswer(invocation -> {
            SiteOnboardingRequest saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        SiteOnboardingResponse response = service.submit(actorId, submission(" Imperial-City ", " Nguyễn Văn A "));

        ArgumentCaptor<SiteOnboardingRequest> saved = ArgumentCaptor.forClass(SiteOnboardingRequest.class);
        verify(siteOnboardingRequestRepository).save(saved.capture());
        assertThat(saved.getValue().getRequesterId()).isEqualTo(actorId);
        assertThat(saved.getValue().getSiteCode()).isEqualTo(SITE_CODE);
        assertThat(saved.getValue().getRepresentativeName()).isEqualTo("Nguyễn Văn A");
        assertThat(saved.getValue().getOrganizationName()).isEqualTo("Trung tâm Bảo tồn Di tích");
        assertThat(saved.getValue().getRepresentativeEmail()).isEqualTo("van.a@ditich.vn");
        assertThat(saved.getValue().getEvidenceNote()).isEqualTo(EVIDENCE);
        assertThat(saved.getValue().getStatus()).isEqualTo(SiteOnboardingStatus.PENDING);
        assertThat(response.requesterId()).isEqualTo(actorId);
        assertThat(response.siteCode()).isEqualTo(SITE_CODE);
        assertThat(response.status()).isEqualTo(SiteOnboardingStatus.PENDING);
        assertThat(response.evidenceNote()).isEqualTo(EVIDENCE);
        verify(adminAuditService, never()).append(any());
    }

    @Test
    void submit_rejectsMissingActor() {
        assertThatThrownBy(() -> service.submit(null, submission(SITE_CODE, "Nguyễn Văn A")))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
        verify(siteOnboardingRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsBlankRepresentativeFields() {
        UUID actorId = UUID.randomUUID();
        assertThatThrownBy(() -> service.submit(actorId, new CreateSiteOnboardingRequest(
                        " ", "Trung tâm Bảo tồn Di tích", "van.a@ditich.vn", SITE_CODE, EVIDENCE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Tên đại diện là bắt buộc");
        assertThatThrownBy(() -> service.submit(actorId, new CreateSiteOnboardingRequest(
                        "Nguyễn Văn A", " ", "van.a@ditich.vn", SITE_CODE, EVIDENCE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Tên tổ chức là bắt buộc");
        assertThatThrownBy(() -> service.submit(actorId, new CreateSiteOnboardingRequest(
                        "Nguyễn Văn A", "Trung tâm Bảo tồn Di tích", " ", SITE_CODE, EVIDENCE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Email đại diện là bắt buộc");
        assertThatThrownBy(() -> service.submit(actorId, new CreateSiteOnboardingRequest(
                        "Nguyễn Văn A", "Trung tâm Bảo tồn Di tích", "van.a@ditich.vn", SITE_CODE, " ")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ghi chú xác minh là bắt buộc");
        verify(siteOnboardingRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsDuplicatePendingBeforeSave() {
        UUID actorId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.of(knownSite()));
        when(siteOnboardingRequestRepository.existsByRequesterIdAndSiteCodeIgnoreCaseAndStatus(
                        actorId, SITE_CODE, SiteOnboardingStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(actorId, submission("Imperial-City", "Nguyễn Văn A")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Đã có yêu cầu onboarding đang chờ duyệt cho site này");

        verify(siteOnboardingRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsUnknownSite() {
        UUID actorId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase("missing-site")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(actorId, submission(" Missing-Site ", "Nguyễn Văn A")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Không tìm thấy site workspace");

        verify(siteOnboardingRequestRepository, never()).existsByRequesterIdAndSiteCodeIgnoreCaseAndStatus(any(), any(), any());
        verify(siteOnboardingRequestRepository, never()).save(any());
    }

    @Test
    void submit_rejectsEvidenceContainingSecretsOrDocuments() {
        UUID actorId = UUID.randomUUID();
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.of(knownSite()));

        for (String evidence : List.of(
                "-----BEGIN PRIVATE KEY-----\nMIIEsecret",
                "data:application/pdf;base64,JVBERi0xLjQK")) {
            assertThatThrownBy(() -> service.submit(actorId, new CreateSiteOnboardingRequest(
                            "Nguyễn Văn A",
                            "Trung tâm Bảo tồn Di tích",
                            "van.a@ditich.vn",
                            SITE_CODE,
                            evidence)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Ghi chú xác minh chỉ được là văn bản");
        }

        verify(siteOnboardingRequestRepository, never()).save(any());
    }

    @Test
    void listMine_returnsCallerRequests() {
        UUID actorId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-24T01:00:00Z");
        SiteOnboardingRequest stored = storedRequest(actorId, createdAt);
        when(siteOnboardingRequestRepository.findByRequesterIdOrderByCreatedAtDesc(actorId))
                .thenReturn(List.of(stored));

        List<SiteOnboardingResponse> result = service.listMine(actorId);

        assertThat(result).containsExactly(toExpected(stored));
    }

    @Test
    void listByStatus_returnsMatchingRequests() {
        UUID actorId = UUID.randomUUID();
        SiteOnboardingRequest stored = storedRequest(actorId, Instant.parse("2026-10-24T01:00:00Z"));
        when(siteOnboardingRequestRepository.findByStatusOrderByCreatedAtAsc(SiteOnboardingStatus.PENDING))
                .thenReturn(List.of(stored));

        List<SiteOnboardingResponse> result = service.listByStatus(SiteOnboardingStatus.PENDING);

        assertThat(result).containsExactly(toExpected(stored));
    }

    @Test
    void approve_createsSiteManagerAndAuditsWithoutSecrets() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        SiteOnboardingRequest pending = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        when(siteOnboardingRequestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.of(knownSite()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, requesterId))
                .thenReturn(Optional.empty());
        when(siteMembershipRepository.save(any(SiteMembership.class))).thenAnswer(invocation -> {
            SiteMembership membership = invocation.getArgument(0);
            membership.setId(UUID.randomUUID());
            return membership;
        });
        when(siteOnboardingRequestRepository.save(pending)).thenReturn(pending);

        SiteOnboardingResponse response = service.approve(pending.getId(), reviewerId, "Hồ sơ hợp lệ");

        ArgumentCaptor<SiteMembership> membership = ArgumentCaptor.forClass(SiteMembership.class);
        verify(siteMembershipRepository).save(membership.capture());
        assertThat(membership.getValue().getSiteCode()).isEqualTo(SITE_CODE);
        assertThat(membership.getValue().getUserId()).isEqualTo(requesterId);
        assertThat(membership.getValue().getRole()).isEqualTo(SiteRole.SITE_MANAGER);
        assertThat(membership.getValue().isActive()).isTrue();
        assertThat(membership.getValue().getGrantedBy()).isEqualTo(reviewerId);
        assertThat(pending.getStatus()).isEqualTo(SiteOnboardingStatus.APPROVED);
        assertThat(pending.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(pending.getReviewNote()).isEqualTo("Hồ sơ hợp lệ");
        assertThat(pending.getReviewedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(SiteOnboardingStatus.APPROVED);
        assertThat(response.reviewNote()).isEqualTo("Hồ sơ hợp lệ");
        assertAuditOmitsEvidence(reviewerId, pending.getId(), "APPROVED", "Hồ sơ hợp lệ");
        assertThat(SiteOnboardingService.class.getMethod("approve", UUID.class, UUID.class, String.class)
                        .getAnnotation(Transactional.class))
                .isNotNull();
    }

    @Test
    void approve_reactivatesExistingMembershipWithoutSecondRow() {
        UUID requesterId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        SiteOnboardingRequest pending = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        SiteMembership existing = SiteMembership.builder()
                .id(membershipId)
                .siteCode(SITE_CODE)
                .userId(requesterId)
                .role(SiteRole.CONTENT_EDITOR)
                .active(false)
                .build();
        when(siteOnboardingRequestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(locationRepository.findBySiteCodeIgnoreCase(SITE_CODE)).thenReturn(Optional.of(knownSite()));
        when(siteMembershipRepository.findBySiteCodeIgnoreCaseAndUserId(SITE_CODE, requesterId))
                .thenReturn(Optional.of(existing));
        when(siteMembershipRepository.save(existing)).thenReturn(existing);
        when(siteOnboardingRequestRepository.save(pending)).thenReturn(pending);

        service.approve(pending.getId(), reviewerId, null);

        verify(siteMembershipRepository).save(existing);
        assertThat(existing.getId()).isEqualTo(membershipId);
        assertThat(existing.getRole()).isEqualTo(SiteRole.SITE_MANAGER);
        assertThat(existing.isActive()).isTrue();
        assertThat(existing.getGrantedBy()).isEqualTo(reviewerId);
        assertThat(pending.getStatus()).isEqualTo(SiteOnboardingStatus.APPROVED);
        assertThat(pending.getReviewNote()).isNull();
    }

    @Test
    void approve_isIdempotentWhenAlreadyApproved() {
        UUID requesterId = UUID.randomUUID();
        UUID originalReviewer = UUID.randomUUID();
        SiteOnboardingRequest approved = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        approved.setStatus(SiteOnboardingStatus.APPROVED);
        approved.setReviewedBy(originalReviewer);
        approved.setReviewNote("Đã xác minh");
        approved.setReviewedAt(Instant.parse("2026-10-24T02:00:00Z"));
        when(siteOnboardingRequestRepository.findById(approved.getId())).thenReturn(Optional.of(approved));

        SiteOnboardingResponse response = service.approve(approved.getId(), UUID.randomUUID(), "Duyệt lại");

        assertThat(response).isEqualTo(toExpected(approved));
        assertThat(approved.getReviewedBy()).isEqualTo(originalReviewer);
        assertThat(approved.getReviewNote()).isEqualTo("Đã xác minh");
        verify(siteOnboardingRequestRepository, never()).save(any());
        verify(siteMembershipRepository, never()).save(any());
        verify(siteMembershipRepository, never()).findBySiteCodeIgnoreCaseAndUserId(any(), any());
        verify(adminAuditService, never()).append(any());
    }

    @Test
    void approve_rejectsUnknownRequest() {
        UUID requestId = UUID.randomUUID();
        when(siteOnboardingRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(requestId, UUID.randomUUID(), "Hồ sơ hợp lệ"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy yêu cầu onboarding");
    }

    @Test
    void approve_rejectsRequestThatIsNotPending() {
        UUID requesterId = UUID.randomUUID();
        SiteOnboardingRequest rejected = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        rejected.setStatus(SiteOnboardingStatus.REJECTED);
        when(siteOnboardingRequestRepository.findById(rejected.getId())).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> service.approve(rejected.getId(), UUID.randomUUID(), "Đổi ý"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Yêu cầu onboarding không còn chờ duyệt");

        verify(siteMembershipRepository, never()).save(any());
        verify(siteOnboardingRequestRepository, never()).save(any());
        verify(adminAuditService, never()).append(any());
    }

    @Test
    void reject_storesReasonAndAuditsWithoutSecrets() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        SiteOnboardingRequest pending = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        when(siteOnboardingRequestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(siteOnboardingRequestRepository.save(pending)).thenReturn(pending);

        SiteOnboardingResponse response = service.reject(pending.getId(), reviewerId, "Thiếu giấy ủy quyền");

        assertThat(pending.getStatus()).isEqualTo(SiteOnboardingStatus.REJECTED);
        assertThat(pending.getReviewedBy()).isEqualTo(reviewerId);
        assertThat(pending.getReviewNote()).isEqualTo("Thiếu giấy ủy quyền");
        assertThat(pending.getReviewedAt()).isNotNull();
        assertThat(response.status()).isEqualTo(SiteOnboardingStatus.REJECTED);
        assertThat(response.reviewNote()).isEqualTo("Thiếu giấy ủy quyền");
        verify(siteMembershipRepository, never()).save(any());
        assertAuditOmitsEvidence(reviewerId, pending.getId(), "REJECTED", "Thiếu giấy ủy quyền");
        assertThat(SiteOnboardingService.class.getMethod("reject", UUID.class, UUID.class, String.class)
                        .getAnnotation(Transactional.class))
                .isNotNull();
    }

    @Test
    void reject_requiresNote() {
        UUID requesterId = UUID.randomUUID();
        SiteOnboardingRequest pending = storedRequest(requesterId, Instant.parse("2026-10-24T01:00:00Z"));
        when(siteOnboardingRequestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        for (String note : new String[] {null, "", "   "}) {
            assertThatThrownBy(() -> service.reject(pending.getId(), UUID.randomUUID(), note))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("Lý do từ chối là bắt buộc");
        }

        verify(siteOnboardingRequestRepository, never()).save(any());
        verify(adminAuditService, never()).append(any());
    }

    private void assertAuditOmitsEvidence(UUID reviewerId, UUID requestId, String action, String reason) {
        ArgumentCaptor<AdminAuditCommand> command = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(adminAuditService).append(command.capture());
        AdminAuditCommand audit = command.getValue();
        assertThat(audit.actorId()).isEqualTo(reviewerId);
        assertThat(audit.entityType()).isEqualTo("SITE_ONBOARDING");
        assertThat(audit.entityId()).isEqualTo(requestId);
        assertThat(audit.action()).isEqualTo(action);
        assertThat(audit.reason()).isEqualTo(reason);
        assertThat(audit.idempotencyKey()).isEqualTo("site-onboarding:" + action + ":" + requestId);
        assertThat(audit.metadata().get("siteCode")).isEqualTo(SITE_CODE);
        assertThat(audit.metadata().keySet())
                .doesNotContain("evidenceNote", "evidence", "document", "binary", "secret", "password", "token", "file");
        assertThat(audit.metadata().values()).noneMatch(value -> String.valueOf(value).contains(EVIDENCE));
        assertThat(audit.reason()).doesNotContain(EVIDENCE);
        assertThat(String.valueOf(audit.metadata())).doesNotContain("BEGIN ", "base64,");
    }

    private CreateSiteOnboardingRequest submission(String siteCode, String representativeName) {
        return new CreateSiteOnboardingRequest(
                representativeName,
                "Trung tâm Bảo tồn Di tích",
                "van.a@ditich.vn",
                siteCode,
                EVIDENCE);
    }

    private Location knownSite() {
        return Location.builder().siteCode(SITE_CODE).name("Hoàng thành").build();
    }

    private SiteOnboardingRequest storedRequest(UUID requesterId, Instant createdAt) {
        return SiteOnboardingRequest.builder()
                .id(UUID.randomUUID())
                .requesterId(requesterId)
                .siteCode(SITE_CODE)
                .representativeName("Nguyễn Văn A")
                .organizationName("Trung tâm Bảo tồn Di tích")
                .representativeEmail("van.a@ditich.vn")
                .evidenceNote(EVIDENCE)
                .status(SiteOnboardingStatus.PENDING)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private SiteOnboardingResponse toExpected(SiteOnboardingRequest request) {
        return new SiteOnboardingResponse(
                request.getId(),
                request.getRequesterId(),
                request.getSiteCode(),
                request.getRepresentativeName(),
                request.getOrganizationName(),
                request.getRepresentativeEmail(),
                request.getEvidenceNote(),
                request.getStatus(),
                request.getReviewedBy(),
                request.getReviewNote(),
                request.getCreatedAt(),
                request.getReviewedAt());
    }
}
