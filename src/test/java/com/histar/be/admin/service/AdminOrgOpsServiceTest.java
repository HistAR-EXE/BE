package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditEventResponse;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.OrgArCardIssueRequest;
import com.histar.be.admin.dto.OrgCertificateBulkRequest;
import com.histar.be.admin.dto.OrgContractRequest;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrganizationArCard;
import com.histar.be.organization.entity.OrganizationCertificate;
import com.histar.be.organization.entity.OrganizationContract;
import com.histar.be.organization.entity.OrganizationMember;
import com.histar.be.organization.repository.OrganizationArCardRepository;
import com.histar.be.organization.repository.OrganizationCertificateRepository;
import com.histar.be.organization.repository.OrganizationContractRepository;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import com.histar.be.visit.repository.VisitSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminOrgOpsServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMemberRepository organizationMemberRepository;
    @Mock private OrganizationContractRepository contractRepository;
    @Mock private OrganizationArCardRepository arCardRepository;
    @Mock private OrganizationCertificateRepository certificateRepository;
    @Mock private HeritageDigitizationInquiryRepository inquiryRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private CheckinRepository checkinRepository;
    @Mock private VisitSessionRepository visitSessionRepository;
    @Mock private UserDiscoveryRepository userDiscoveryRepository;
    @Mock private UserQuestProgressRepository userQuestProgressRepository;
    @Mock private AdminAuditService audit;
    @Mock private CurrentUserAccessor currentUserAccessor;

    private AdminOrgOpsService service;
    private UUID orgId;
    private UUID actorId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new AdminOrgOpsService(
                organizationRepository,
                organizationMemberRepository,
                contractRepository,
                arCardRepository,
                certificateRepository,
                inquiryRepository,
                profileRepository,
                checkinRepository,
                visitSessionRepository,
                userDiscoveryRepository,
                userQuestProgressRepository,
                audit,
                currentUserAccessor);
        orgId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    private Organization org() {
        return Organization.builder()
                .id(orgId)
                .name("Demo School")
                .contactEmail("contact@demo.edu.vn")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void createContractPersistsAndAudits() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org()));
        when(contractRepository.existsByOrganizationIdAndContractCodeIgnoreCase(orgId, "CTR-1"))
                .thenReturn(false);
        when(contractRepository.save(any(OrganizationContract.class))).thenAnswer(inv -> {
            OrganizationContract c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        var response = service.createContract(
                orgId,
                new OrgContractRequest(
                        "Hợp đồng demo", "CTR-1", 30, "STANDARD", "ACTIVE", null, null, null, null));

        assertThat(response.contractCode()).isEqualTo("CTR-1");
        assertThat(response.status()).isEqualTo("ACTIVE");
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("ORG_CONTRACT_CREATED");
    }

    @Test
    void cancelContractSoftCancels() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        UUID contractId = UUID.randomUUID();
        OrganizationContract existing = OrganizationContract.builder()
                .id(contractId)
                .organizationId(orgId)
                .title("HĐ")
                .contractCode("CTR-X")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(contractRepository.findByIdAndOrganizationId(contractId, orgId)).thenReturn(Optional.of(existing));
        when(contractRepository.save(any(OrganizationContract.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = service.cancelContract(orgId, contractId);

        assertThat(response.status()).isEqualTo("CANCELLED");
        verify(audit).append(any(AdminAuditCommand.class));
    }

    @Test
    void syncCrmLinksMatchingInquiries() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        Organization organization = org();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        HeritageDigitizationInquiry inquiry = HeritageDigitizationInquiry.builder()
                .id(UUID.randomUUID())
                .siteName("Demo School Campus")
                .contactName("A")
                .contactEmail("contact@demo.edu.vn")
                .packageType("STANDARD")
                .status("NEW")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(inquiryRepository.findCrmMatchCandidates("contact@demo.edu.vn", "Demo School"))
                .thenReturn(List.of(inquiry));
        when(inquiryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.syncCrm(orgId);

        assertThat(result.linkedCount()).isEqualTo(1);
        assertThat(inquiry.getOrganizationId()).isEqualTo(orgId);
        assertThat(organization.getLastCrmSyncedAt()).isNotNull();
        assertThat(organization.getCrmLinkedInquiryCount()).isEqualTo(1);
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("ORG_CRM_SYNCED");
    }

    @Test
    void syncCrmDoesNotRelinkInquiryOwnedByAnotherOrganization() {
        UUID foreignOrgId = UUID.randomUUID();
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        Organization organization = org();
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        HeritageDigitizationInquiry foreignInquiry = HeritageDigitizationInquiry.builder()
                .id(UUID.randomUUID())
                .siteName("Demo School Campus")
                .contactName("A")
                .contactEmail("contact@demo.edu.vn")
                .packageType("STANDARD")
                .status("NEW")
                .organizationId(foreignOrgId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(inquiryRepository.findCrmMatchCandidates("contact@demo.edu.vn", "Demo School"))
                .thenReturn(List.of(foreignInquiry));
        when(organizationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.syncCrm(orgId);

        assertThat(result.linkedCount()).isZero();
        assertThat(foreignInquiry.getOrganizationId()).isEqualTo(foreignOrgId);
        verify(inquiryRepository, never()).save(foreignInquiry);
    }

    @Test
    void issueArCardsSkipsExistingActive() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org()));
        when(organizationMemberRepository.findByOrganizationId(orgId))
                .thenReturn(List.of(OrganizationMember.builder()
                        .id(UUID.randomUUID())
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("MEMBER")
                        .invitedAt(Instant.now())
                        .build()));
        when(arCardRepository.existsByOrganizationIdAndUserIdAndStatusIgnoreCase(orgId, userId, "ACTIVE"))
                .thenReturn(true);
        when(profileRepository.findAllById(List.of(userId))).thenReturn(List.of(Profile.builder()
                .id(userId)
                .email("m@demo.edu.vn")
                .displayName("Member")
                .build()));

        var result = service.issueArCards(orgId, new OrgArCardIssueRequest(List.of(userId)));

        assertThat(result.issued()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        verify(arCardRepository, never()).save(any());
    }

    @Test
    void issueArCardsCreatesWhenMissing() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org()));
        when(organizationMemberRepository.findByOrganizationId(orgId))
                .thenReturn(List.of(OrganizationMember.builder()
                        .id(UUID.randomUUID())
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("MEMBER")
                        .invitedAt(Instant.now())
                        .build()));
        when(arCardRepository.existsByOrganizationIdAndUserIdAndStatusIgnoreCase(orgId, userId, "ACTIVE"))
                .thenReturn(false);
        when(profileRepository.findAllById(List.of(userId))).thenReturn(List.of(Profile.builder()
                .id(userId)
                .email("m@demo.edu.vn")
                .displayName("Member")
                .build()));
        when(arCardRepository.save(any(OrganizationArCard.class))).thenAnswer(inv -> {
            OrganizationArCard card = inv.getArgument(0);
            card.setId(UUID.randomUUID());
            return card;
        });

        var result = service.issueArCards(orgId, null);

        assertThat(result.issued()).isEqualTo(1);
        assertThat(result.skipped()).isZero();
        assertThat(result.cards()).hasSize(1);
    }

    @Test
    void issueCertificatesBulkForMembers() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org()));
        when(organizationMemberRepository.findByOrganizationId(orgId))
                .thenReturn(List.of(OrganizationMember.builder()
                        .id(UUID.randomUUID())
                        .organizationId(orgId)
                        .userId(userId)
                        .orgRole("MEMBER")
                        .invitedAt(Instant.now())
                        .build()));
        when(profileRepository.findAllById(List.of(userId))).thenReturn(List.of(Profile.builder()
                .id(userId)
                .email("m@demo.edu.vn")
                .displayName("Member")
                .build()));
        when(userQuestProgressRepository.countByUserIdAndStatus(userId, "completed")).thenReturn(3L);
        when(certificateRepository.save(any(OrganizationCertificate.class))).thenAnswer(inv -> {
            OrganizationCertificate cert = inv.getArgument(0);
            cert.setId(UUID.randomUUID());
            return cert;
        });

        var result = service.issueCertificates(orgId, new OrgCertificateBulkRequest(null, "Chứng nhận AR"));

        assertThat(result.issued()).isEqualTo(1);
        assertThat(result.certificates().get(0).title()).isEqualTo("Chứng nhận AR");
        ArgumentCaptor<AdminAuditCommand> captor = ArgumentCaptor.forClass(AdminAuditCommand.class);
        verify(audit).append(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("ORG_CERTIFICATES_ISSUED");
    }

    @Test
    void activityLogsDelegatesToAuditWithOrgEntityId() {
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org()));
        PageResponse<AdminAuditEventResponse> page = PageResponse.<AdminAuditEventResponse>builder()
                .items(List.of())
                .page(0)
                .size(10)
                .totalItems(0)
                .totalPages(0)
                .build();
        when(audit.list(eq(0), eq(10), eq("ORGANIZATION"), eq(null), eq(null), eq(null), eq(orgId), eq(null), eq(null), eq("desc")))
                .thenReturn(page);

        var result = service.activityLogs(orgId, 0, 10);

        assertThat(result.getTotalItems()).isZero();
        verify(audit)
                .list(0, 10, "ORGANIZATION", null, null, null, orgId, null, null, "desc");
    }
}
