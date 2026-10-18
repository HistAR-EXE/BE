package com.histar.be.admin.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditEventResponse;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.OrgArCardIssueRequest;
import com.histar.be.admin.dto.OrgArCardIssueResult;
import com.histar.be.admin.dto.OrgArCardResponse;
import com.histar.be.admin.dto.OrgArReportResponse;
import com.histar.be.admin.dto.OrgCertificateBulkRequest;
import com.histar.be.admin.dto.OrgCertificateBulkResult;
import com.histar.be.admin.dto.OrgCertificateResponse;
import com.histar.be.admin.dto.OrgContractRequest;
import com.histar.be.admin.dto.OrgContractResponse;
import com.histar.be.admin.dto.OrgCrmSyncResponse;
import com.histar.be.billing.entity.HeritageDigitizationInquiry;
import com.histar.be.billing.repository.HeritageDigitizationInquiryRepository;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
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
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminOrgOpsService {

    private static final int BULK_MAX = 200;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationContractRepository contractRepository;
    private final OrganizationArCardRepository arCardRepository;
    private final OrganizationCertificateRepository certificateRepository;
    private final HeritageDigitizationInquiryRepository inquiryRepository;
    private final ProfileRepository profileRepository;
    private final CheckinRepository checkinRepository;
    private final VisitSessionRepository visitSessionRepository;
    private final UserDiscoveryRepository userDiscoveryRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final AdminAuditService audit;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public List<OrgContractResponse> listContracts(UUID orgId) {
        requireOrg(orgId);
        return contractRepository.findByOrganizationIdOrderByUpdatedAtDesc(orgId).stream()
                .map(OrgContractResponse::from)
                .toList();
    }

    @Transactional
    public OrgContractResponse createContract(UUID orgId, OrgContractRequest request) {
        UUID actorId = requireActor();
        requireOrg(orgId);
        String code = request.contractCode().trim();
        if (contractRepository.existsByOrganizationIdAndContractCodeIgnoreCase(orgId, code)) {
            throw new ConflictException("Mã hợp đồng đã tồn tại trong tổ chức");
        }
        Instant now = Instant.now();
        OrganizationContract saved = contractRepository.save(OrganizationContract.builder()
                .organizationId(orgId)
                .title(request.title().trim())
                .contractCode(code)
                .licenseSeats(request.licenseSeats())
                .planType(blankToNull(request.planType()))
                .status(normalizeStatus(request.status(), "DRAFT"))
                .startsOn(request.startsOn())
                .endsOn(request.endsOn())
                .documentUrl(blankToNull(request.documentUrl()))
                .notes(blankToNull(request.notes()))
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actorId)
                .build());
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_CONTRACT_CREATED",
                null,
                null,
                "Tạo hợp đồng " + saved.getContractCode(),
                null,
                Map.of("contractId", saved.getId().toString(), "orgId", orgId.toString())));
        return OrgContractResponse.from(saved);
    }

    @Transactional
    public OrgContractResponse updateContract(UUID orgId, UUID contractId, OrgContractRequest request) {
        UUID actorId = requireActor();
        OrganizationContract contract = contractRepository
                .findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        String code = request.contractCode().trim();
        if (!code.equalsIgnoreCase(contract.getContractCode())
                && contractRepository.existsByOrganizationIdAndContractCodeIgnoreCase(orgId, code)) {
            throw new ConflictException("Mã hợp đồng đã tồn tại trong tổ chức");
        }
        contract.setTitle(request.title().trim());
        contract.setContractCode(code);
        contract.setLicenseSeats(request.licenseSeats());
        contract.setPlanType(blankToNull(request.planType()));
        contract.setStatus(normalizeStatus(request.status(), contract.getStatus()));
        contract.setStartsOn(request.startsOn());
        contract.setEndsOn(request.endsOn());
        contract.setDocumentUrl(blankToNull(request.documentUrl()));
        contract.setNotes(blankToNull(request.notes()));
        contract.setUpdatedAt(Instant.now());
        OrganizationContract saved = contractRepository.save(contract);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_CONTRACT_UPDATED",
                null,
                null,
                "Cập nhật hợp đồng " + saved.getContractCode(),
                null,
                Map.of("contractId", saved.getId().toString(), "orgId", orgId.toString())));
        return OrgContractResponse.from(saved);
    }

    @Transactional
    public OrgContractResponse cancelContract(UUID orgId, UUID contractId) {
        UUID actorId = requireActor();
        OrganizationContract contract = contractRepository
                .findByIdAndOrganizationId(contractId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found"));
        contract.setStatus("CANCELLED");
        contract.setUpdatedAt(Instant.now());
        OrganizationContract saved = contractRepository.save(contract);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_CONTRACT_CANCELLED",
                null,
                null,
                "Hủy hợp đồng " + saved.getContractCode(),
                null,
                Map.of("contractId", saved.getId().toString(), "orgId", orgId.toString())));
        return OrgContractResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditEventResponse> activityLogs(UUID orgId, int page, int size) {
        requireOrg(orgId);
        return audit.list(page, size, "ORGANIZATION", null, null, null, orgId, null, null, "desc");
    }

    @Transactional(readOnly = true)
    public OrgArReportResponse arReport(UUID orgId, Instant from, Instant to) {
        Organization org = requireOrg(orgId);
        Instant rangeTo = to == null ? Instant.now() : to;
        Instant rangeFrom = from == null ? rangeTo.minus(30, ChronoUnit.DAYS) : from;
        List<UUID> memberIds = memberUserIds(orgId);
        long memberCount = memberIds.size();
        if (memberIds.isEmpty()) {
            return new OrgArReportResponse(
                    orgId, org.getName(), rangeFrom, rangeTo, 0, 0, 0, 0, 0, 0, List.of());
        }
        List<Checkin> checkins = checkinRepository.findByUserIdInAndCreatedAtGreaterThanEqual(memberIds, rangeFrom);
        long checkinCount = checkins.stream()
                .filter(c -> c.getCreatedAt() == null || !c.getCreatedAt().isAfter(rangeTo))
                .count();
        long sessions = visitSessionRepository.countByUserIdInAndStartedAtGreaterThanEqual(memberIds, rangeFrom);
        long onsite = visitSessionRepository.countByUserIdInAndModeIgnoreCaseAndStartedAtGreaterThanEqual(
                memberIds, "onsite", rangeFrom);
        // mode offline also counts as onsite UX in product; include both
        long offline = visitSessionRepository.countByUserIdInAndModeIgnoreCaseAndStartedAtGreaterThanEqual(
                memberIds, "offline", rangeFrom);
        long discoveries =
                userDiscoveryRepository.countByUserIdInAndDiscoveredAtGreaterThanEqual(memberIds, rangeFrom);
        long questCompletions = userQuestProgressRepository.countByUserIdInAndStatusIgnoreCaseAndCompletedAtGreaterThanEqual(
                memberIds, "completed", rangeFrom);

        Map<String, Long> byDay = new HashMap<>();
        for (Checkin c : checkins) {
            if (c.getCreatedAt() == null || c.getCreatedAt().isAfter(rangeTo)) {
                continue;
            }
            String day = LocalDate.ofInstant(c.getCreatedAt(), ZoneOffset.UTC).format(DAY);
            byDay.merge(day, 1L, Long::sum);
        }
        List<OrgArReportResponse.DailyPoint> daily = byDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new OrgArReportResponse.DailyPoint(e.getKey(), e.getValue()))
                .toList();

        return new OrgArReportResponse(
                orgId,
                org.getName(),
                rangeFrom,
                rangeTo,
                memberCount,
                checkinCount,
                sessions,
                onsite + offline,
                discoveries,
                questCompletions,
                daily);
    }

    @Transactional
    public OrgCrmSyncResponse syncCrm(UUID orgId) {
        UUID actorId = requireActor();
        Organization org = requireOrg(orgId);
        String email = org.getContactEmail() == null ? "" : org.getContactEmail().trim();
        String name = org.getName() == null ? "" : org.getName().trim();
        List<HeritageDigitizationInquiry> matches =
                inquiryRepository.findCrmMatchCandidates(email, name);
        Instant now = Instant.now();
        List<OrgCrmSyncResponse.LinkedInquiry> linked = new ArrayList<>();
        for (HeritageDigitizationInquiry inquiry : matches) {
            if (inquiry.getOrganizationId() != null && !orgId.equals(inquiry.getOrganizationId())) {
                continue;
            }
            inquiry.setOrganizationId(orgId);
            inquiry.setUpdatedAt(now);
            inquiryRepository.save(inquiry);
            linked.add(new OrgCrmSyncResponse.LinkedInquiry(
                    inquiry.getId(), inquiry.getSiteName(), inquiry.getContactEmail(), inquiry.getStatus()));
        }
        org.setLastCrmSyncedAt(now);
        org.setCrmLinkedInquiryCount(linked.size());
        organizationRepository.save(org);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_CRM_SYNCED",
                null,
                null,
                "Đồng bộ CRM: " + linked.size() + " inquiry",
                null,
                Map.of("linkedCount", linked.size(), "orgId", orgId.toString())));
        return new OrgCrmSyncResponse(orgId, linked.size(), now, linked);
    }

    @Transactional(readOnly = true)
    public List<OrgArCardResponse> listArCards(UUID orgId) {
        requireOrg(orgId);
        List<OrganizationArCard> cards = arCardRepository.findByOrganizationIdOrderByIssuedAtDesc(orgId);
        Map<UUID, Profile> profiles = profilesByIds(
                cards.stream().map(OrganizationArCard::getUserId).distinct().toList());
        return cards.stream().map(c -> toCardResponse(c, profiles.get(c.getUserId()))).toList();
    }

    @Transactional
    public OrgArCardIssueResult issueArCards(UUID orgId, OrgArCardIssueRequest request) {
        UUID actorId = requireActor();
        requireOrg(orgId);
        List<UUID> targets = resolveMemberTargets(orgId, request == null ? null : request.userIds());
        int issued = 0;
        int skipped = 0;
        List<OrgArCardResponse> created = new ArrayList<>();
        Instant now = Instant.now();
        Map<UUID, Profile> profiles = profilesByIds(targets);
        for (UUID userId : targets) {
            if (arCardRepository.existsByOrganizationIdAndUserIdAndStatusIgnoreCase(orgId, userId, "ACTIVE")) {
                skipped++;
                continue;
            }
            OrganizationArCard card = arCardRepository.save(OrganizationArCard.builder()
                    .organizationId(orgId)
                    .userId(userId)
                    .cardCode(uniqueCode("AR"))
                    .status("ACTIVE")
                    .issuedAt(now)
                    .issuedBy(actorId)
                    .build());
            created.add(toCardResponse(card, profiles.get(userId)));
            issued++;
        }
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_AR_CARDS_ISSUED",
                null,
                null,
                "Cấp thẻ AR: " + issued + " mới, bỏ qua " + skipped,
                null,
                Map.of("issued", issued, "skipped", skipped, "orgId", orgId.toString())));
        return new OrgArCardIssueResult(issued, skipped, created);
    }

    @Transactional
    public OrgArCardResponse revokeArCard(UUID orgId, UUID cardId) {
        UUID actorId = requireActor();
        OrganizationArCard card = arCardRepository
                .findByIdAndOrganizationId(cardId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("AR card not found"));
        if ("REVOKED".equalsIgnoreCase(card.getStatus())) {
            Profile p = profileRepository.findById(card.getUserId()).orElse(null);
            return toCardResponse(card, p);
        }
        card.setStatus("REVOKED");
        card.setRevokedAt(Instant.now());
        OrganizationArCard saved = arCardRepository.save(card);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_AR_CARD_REVOKED",
                null,
                null,
                "Thu hồi thẻ AR " + saved.getCardCode(),
                null,
                Map.of("cardId", saved.getId().toString(), "orgId", orgId.toString())));
        Profile p = profileRepository.findById(saved.getUserId()).orElse(null);
        return toCardResponse(saved, p);
    }

    @Transactional(readOnly = true)
    public List<OrgCertificateResponse> listCertificates(UUID orgId) {
        requireOrg(orgId);
        List<OrganizationCertificate> certs =
                certificateRepository.findByOrganizationIdOrderByIssuedAtDesc(orgId);
        Map<UUID, Profile> profiles = profilesByIds(
                certs.stream().map(OrganizationCertificate::getUserId).distinct().toList());
        return certs.stream().map(c -> toCertResponse(c, profiles.get(c.getUserId()))).toList();
    }

    @Transactional
    public OrgCertificateBulkResult issueCertificates(UUID orgId, OrgCertificateBulkRequest request) {
        UUID actorId = requireActor();
        requireOrg(orgId);
        String title = request == null || request.title() == null || request.title().isBlank()
                ? "Chứng nhận trải nghiệm AR HistAR"
                : request.title().trim();
        List<UUID> targets = resolveMemberTargets(orgId, request == null ? null : request.userIds());
        Instant now = Instant.now();
        Map<UUID, Profile> profiles = profilesByIds(targets);
        List<OrgCertificateResponse> created = new ArrayList<>();
        for (UUID userId : targets) {
            long completed = userQuestProgressRepository.countByUserIdAndStatus(userId, "completed");
            OrganizationCertificate cert = certificateRepository.save(OrganizationCertificate.builder()
                    .organizationId(orgId)
                    .userId(userId)
                    .title(title)
                    .certificateCode(uniqueCode("CERT"))
                    .issuedAt(now)
                    .issuedBy(actorId)
                    .metadataJson("{\"questCompletions\":" + completed + "}")
                    .build());
            created.add(toCertResponse(cert, profiles.get(userId)));
        }
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_CERTIFICATES_ISSUED",
                null,
                null,
                "Cấp chứng nhận hàng loạt: " + created.size(),
                null,
                Map.of("issued", created.size(), "orgId", orgId.toString())));
        return new OrgCertificateBulkResult(created.size(), created);
    }

    @Transactional(readOnly = true)
    public byte[] exportCertificatesCsv(UUID orgId) {
        List<OrgCertificateResponse> rows = listCertificates(orgId);
        StringBuilder sb = new StringBuilder();
        sb.append("id,email,displayName,title,certificateCode,issuedAt\n");
        for (OrgCertificateResponse row : rows) {
            sb.append(csv(row.id().toString()))
                    .append(',')
                    .append(csv(row.email()))
                    .append(',')
                    .append(csv(row.displayName()))
                    .append(',')
                    .append(csv(row.title()))
                    .append(',')
                    .append(csv(row.certificateCode()))
                    .append(',')
                    .append(row.issuedAt() == null ? "" : row.issuedAt().toString())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<UUID> resolveMemberTargets(UUID orgId, List<UUID> requested) {
        Set<UUID> members = new HashSet<>(memberUserIds(orgId));
        if (members.isEmpty()) {
            throw new BusinessRuleException("Tổ chức chưa có thành viên");
        }
        if (requested == null || requested.isEmpty()) {
            return List.copyOf(members);
        }
        if (requested.size() > BULK_MAX) {
            throw new BusinessRuleException("Tối đa " + BULK_MAX + " người mỗi lần");
        }
        List<UUID> targets = new ArrayList<>();
        for (UUID id : requested) {
            if (!members.contains(id)) {
                throw new BusinessRuleException("User không thuộc tổ chức: " + id);
            }
            targets.add(id);
        }
        return targets;
    }

    private List<UUID> memberUserIds(UUID orgId) {
        return organizationMemberRepository.findByOrganizationId(orgId).stream()
                .map(OrganizationMember::getUserId)
                .toList();
    }

    private Map<UUID, Profile> profilesByIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Profile::getId, p -> p));
    }

    private OrgArCardResponse toCardResponse(OrganizationArCard card, Profile profile) {
        return new OrgArCardResponse(
                card.getId(),
                card.getOrganizationId(),
                card.getUserId(),
                profile == null ? "" : profile.getEmail(),
                profile == null ? "" : profile.getDisplayName(),
                card.getCardCode(),
                card.getStatus(),
                card.getIssuedAt(),
                card.getRevokedAt());
    }

    private OrgCertificateResponse toCertResponse(OrganizationCertificate cert, Profile profile) {
        return new OrgCertificateResponse(
                cert.getId(),
                cert.getOrganizationId(),
                cert.getUserId(),
                profile == null ? "" : profile.getEmail(),
                profile == null ? "" : profile.getDisplayName(),
                cert.getTitle(),
                cert.getCertificateCode(),
                cert.getIssuedAt());
    }

    private Organization requireOrg(UUID orgId) {
        return organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
    }

    private UUID requireActor() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }

    private static String normalizeStatus(String status, String fallback) {
        if (status == null || status.isBlank()) {
            return fallback;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + Long.toHexString(RANDOM.nextLong()).toUpperCase(Locale.ROOT).replace("-", "");
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
