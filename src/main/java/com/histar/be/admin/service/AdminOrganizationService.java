package com.histar.be.admin.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.AdminOrgMemberRequest;
import com.histar.be.admin.dto.AdminOrgMemberResponse;
import com.histar.be.admin.dto.AdminOrganizationSummaryResponse;
import com.histar.be.admin.dto.AdminRosterImportResult;
import com.histar.be.admin.dto.CreateAdminOrganizationRequest;
import com.histar.be.admin.dto.UpdateAdminOrganizationRequest;
import com.histar.be.billing.policy.OrgPlanLimits;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.mail.EmailDeliveryException;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.organization.dto.OrgInviteCodeResponse;
import com.histar.be.organization.dto.OrgRosterMemberResponse;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.repository.OrganizationMemberRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrgInviteService;
import com.histar.be.organization.service.OrgMembershipService;
import com.histar.be.organization.service.OrganizationAnalyticsService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrganizationService {

    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationAnalyticsService organizationAnalyticsService;
    private final OrgInviteService orgInviteService;
    private final OrgMembershipService orgMembershipService;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final HistarEmailService histarEmailService;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;
    private final AdminAuditService audit;
    private final CurrentUserAccessor currentUserAccessor;

    @Transactional(readOnly = true)
    public PageResponse<AdminOrganizationSummaryResponse> list(
            int page, int size, String q, String plan, String status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Organization> result = organizationRepository.findAdminOrganizations(
                blankToEmpty(q),
                upperOrEmpty(plan),
                upperOrEmpty(status),
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id"))));
        return PageResponse.<AdminOrganizationSummaryResponse>builder()
                .items(result.getContent().stream().map(this::toSummary).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public AdminOrganizationSummaryResponse create(CreateAdminOrganizationRequest request) {
        UUID actorId = requireActor();
        String name = request.name().trim();
        OrgSubscription plan = resolvePlan(request.planType());
        OrgPlanLimits.Limits limits = OrgPlanLimits.forPlan(plan);
        String status = blankToEmpty(request.status()).isEmpty()
                ? "ACTIVE"
                : request.status().trim().toUpperCase(Locale.ROOT);

        Organization org = organizationRepository.save(Organization.builder()
                .name(name)
                .slug(slugify(name))
                .plan(plan.name().toLowerCase(Locale.ROOT))
                .planType(plan.name())
                .contactEmail(blankToNull(request.contactEmail()))
                .status(status)
                .maxCcu(limits.maxCcu())
                .maxVerifiedAccounts(limits.maxVerifiedAccounts())
                .maxAiQueriesPerMonth(limits.maxAiQueriesPerMonth())
                .planStartDate(LocalDate.now(VN_ZONE))
                .createdAt(Instant.now())
                .build());

        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                org.getId(),
                "ORG_CREATED",
                null,
                null,
                "Admin tạo tổ chức mới",
                null,
                Map.of("name", name, "planType", plan.name(), "status", status)));
        return toSummary(org);
    }

    @Transactional
    public AdminOrganizationSummaryResponse update(UUID orgId, UpdateAdminOrganizationRequest request) {
        UUID actorId = requireActor();
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        if (request.name() != null && !request.name().isBlank()) {
            org.setName(request.name().trim());
        }
        if (request.planType() != null && !request.planType().isBlank()) {
            OrgSubscription plan = resolvePlan(request.planType());
            OrgPlanLimits.Limits limits = OrgPlanLimits.forPlan(plan);
            org.setPlan(plan.name().toLowerCase(Locale.ROOT));
            org.setPlanType(plan.name());
            org.setMaxCcu(limits.maxCcu());
            org.setMaxVerifiedAccounts(limits.maxVerifiedAccounts());
            org.setMaxAiQueriesPerMonth(limits.maxAiQueriesPerMonth());
        }
        if (request.contactEmail() != null) {
            org.setContactEmail(blankToNull(request.contactEmail()));
        }
        if (request.status() != null && !request.status().isBlank()) {
            org.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        }
        if (request.opsNote() != null) {
            String note = request.opsNote().trim();
            org.setOpsNote(note.isEmpty() ? null : note);
        }
        Organization saved = organizationRepository.save(org);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_UPDATED",
                null,
                null,
                "Admin cập nhật tổ chức",
                null,
                Map.of("name", saved.getName() == null ? "" : saved.getName())));
        return toSummary(saved);
    }

    @Transactional
    public OrgInviteCodeResponse generateInvite(UUID orgId) {
        UUID actorId = requireActor();
        OrgInviteCodeResponse invite = orgInviteService.generateInviteCodeAsAdmin(orgId);
        audit.append(new AdminAuditCommand(
                actorId,
                "ORGANIZATION",
                orgId,
                "ORG_INVITE_GENERATED",
                null,
                null,
                "Admin tạo mã mời tổ chức",
                null,
                Map.of("inviteCode", invite.inviteCode() == null ? "" : invite.inviteCode())));
        return invite;
    }

    @Transactional
    public AdminOrgMemberResponse addMember(UUID orgId, AdminOrgMemberRequest request) {
        requireActor();
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        return provisionMember(orgId, request.email(), request.displayName(), request.sendCredentialsEmail());
    }

    @Transactional(readOnly = true)
    public byte[] exportRosterCsv(UUID orgId) {
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        List<OrgRosterMemberResponse> roster = organizationAnalyticsService.roster(orgId);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("email,displayName,orgRole,level,totalPoints,questsCompleted\n");
        for (OrgRosterMemberResponse row : roster) {
            sb.append(csv(row.email()))
                    .append(',')
                    .append(csv(row.displayName()))
                    .append(',')
                    .append(csv(row.orgRole()))
                    .append(',')
                    .append(row.level())
                    .append(',')
                    .append(row.totalPoints())
                    .append(',')
                    .append(row.questsCompleted())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public AdminRosterImportResult importRosterCsv(UUID orgId, MultipartFile file) {
        requireActor();
        organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("File CSV trống");
        }
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessRuleException("Không đọc được file CSV");
        }
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        int created = 0;
        int enrolled = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        String[] lines = text.split("\\R");
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = splitCsvLine(line);
            if (parts.length == 0) {
                continue;
            }
            String email = parts[0].trim();
            if (lineNo == 1 && email.equalsIgnoreCase("email")) {
                continue;
            }
            if (!email.contains("@")) {
                errors.add("Dòng " + lineNo + ": email không hợp lệ");
                skipped++;
                continue;
            }
            String displayName = parts.length > 1 ? parts[1].trim() : null;
            try {
                AdminOrgMemberResponse result = provisionMember(orgId, email, displayName, false);
                if (result.accountCreated()) {
                    created++;
                } else {
                    enrolled++;
                }
            } catch (Exception ex) {
                errors.add("Dòng " + lineNo + ": " + ex.getMessage());
                skipped++;
            }
        }
        return new AdminRosterImportResult(created, enrolled, skipped, errors);
    }

    private AdminOrgMemberResponse provisionMember(
            UUID orgId, String emailRaw, String displayNameRaw, Boolean sendCredentialsEmail) {
        String email = emailRaw.trim().toLowerCase(Locale.ROOT);
        String displayName = resolveDisplayName(displayNameRaw, email);
        boolean sendEmail = sendCredentialsEmail == null || sendCredentialsEmail;

        Optional<Profile> existing = profileRepository.findByEmail(email);
        if (existing.isPresent()) {
            Profile profile = existing.get();
            if (profile.getOrgId() != null && !profile.getOrgId().equals(orgId)) {
                throw new BusinessRuleException(
                        "Email đã thuộc tổ chức khác. Người dùng cần rời tổ chức hiện tại trước.");
            }
            orgMembershipService.enrollUserInOrganization(profile.getId(), orgId);
            return new AdminOrgMemberResponse(
                    profile.getId(), profile.getEmail(), profile.getDisplayName(), false, false, null);
        }

        orgMembershipService.assertOrgHasSeat(orgId);
        String tempPassword = randomPassword(12);
        Profile profile = Profile.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .displayName(displayName)
                .provider("local")
                .role(UserRole.USER.name())
                .tier(UserTier.FREE.name())
                .orgSubscription(OrgSubscription.NONE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build();
        profileRepository.save(profile);
        orgMembershipService.enrollUserInOrganization(profile.getId(), orgId);

        boolean emailed = false;
        if (sendEmail) {
            Organization org = organizationRepository.findById(orgId).orElseThrow();
            emailed = dispatchCredentialsEmail(org.getName(), email, displayName, tempPassword);
        }
        return new AdminOrgMemberResponse(
                profile.getId(), email, displayName, true, emailed, emailed ? null : tempPassword);
    }

    private boolean dispatchCredentialsEmail(String orgName, String to, String studentName, String tempPassword) {
        String loginUrl = appProperties.getFrontendUrl().replaceAll("/$", "") + "/login";
        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — admin provision credentials for {} (org {})", to, orgName);
            return false;
        }
        try {
            String html =
                    """
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Quản trị HistAR đã tạo tài khoản cho bạn tham gia tổ chức <strong>%s</strong>.</p>
                    <ul>
                      <li>Email đăng nhập: <strong>%s</strong></li>
                      <li>Mật khẩu tạm: <strong>%s</strong></li>
                    </ul>
                    <p><a href="%s">Đăng nhập HistAR</a></p>
                    """
                            .formatted(studentName, orgName, to, tempPassword, loginUrl);
            histarEmailService.sendHtml(to, "Tài khoản HistAR — " + orgName, html);
            return true;
        } catch (EmailDeliveryException ex) {
            throw new BusinessRuleException("Không gửi được email thông tin đăng nhập. Thử lại sau.");
        }
    }

    private AdminOrganizationSummaryResponse toSummary(Organization org) {
        long memberCount = organizationMemberRepository.countByOrganizationId(org.getId());
        String planType = org.getPlanType() != null ? org.getPlanType() : org.getPlan();
        return new AdminOrganizationSummaryResponse(
                org.getId(),
                org.getName(),
                null,
                planType,
                org.getPlan(),
                memberCount,
                org.getStatus());
    }

    private UUID requireActor() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }

    private static OrgSubscription resolvePlan(String planType) {
        if (planType == null || planType.isBlank()) {
            return OrgSubscription.STANDARD;
        }
        String upper = planType.trim().toUpperCase(Locale.ROOT);
        if ("B2B".equals(upper) || "B2B2C".equals(upper)) {
            return OrgSubscription.STANDARD;
        }
        OrgSubscription fromType = OrgSubscription.fromPlanType(planType);
        return fromType == OrgSubscription.NONE ? OrgSubscription.STANDARD : fromType;
    }

    private static String slugify(String name) {
        String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "org";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String resolveDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private static String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts.toArray(String[]::new);
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value.replace("\"", "\"\"");
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            return "\"" + safe + "\"";
        }
        return safe;
    }

    private static String blankToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = blankToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upperOrEmpty(String value) {
        String trimmed = blankToEmpty(value);
        return trimmed.isEmpty() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }
}
