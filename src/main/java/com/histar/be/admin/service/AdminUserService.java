package com.histar.be.admin.service;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.AdminResendVerificationResponse;
import com.histar.be.admin.dto.AdminUserBulkRequest;
import com.histar.be.admin.dto.AdminUserBulkResponse;
import com.histar.be.admin.dto.AdminUserSummaryResponse;
import com.histar.be.admin.dto.CreateAdminUserRequest;
import com.histar.be.admin.dto.CreateAdminUserResponse;
import com.histar.be.admin.dto.ForceVerifyRequest;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.admin.dto.UpdateUserStatusRequest;
import com.histar.be.admin.dto.UpdateUserTierRequest;
import com.histar.be.auth.service.EmailVerificationService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.mail.EmailDeliveryException;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.AccountStatus;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final int EXPORT_MAX_ROWS = 5000;
    private static final int BULK_MAX = 100;

    private final ProfileRepository profileRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AdminAuditService audit;
    private final PasswordEncoder passwordEncoder;
    private final HistarEmailService histarEmailService;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;
    private final EmailVerificationService emailVerificationService;

    public PageResponse<AdminUserSummaryResponse> listUsers(int page, int size) {
        return listUsers(page, size, null, null, null, null, null);
    }

    public PageResponse<AdminUserSummaryResponse> listUsers(
            int page, int size, String q, String role, String tier, Boolean verified, String status) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String normalizedQ = blankToEmpty(q);
        String normalizedRole = upperOrEmpty(role);
        String normalizedTier = upperOrEmpty(tier);
        String normalizedStatus = upperOrEmpty(status);
        Page<Profile> result = profileRepository.findAdminUsers(
                normalizedQ,
                normalizedRole,
                normalizedTier,
                verified,
                normalizedStatus,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.<AdminUserSummaryResponse>builder()
                .items(result.getContent().stream().map(AdminUserSummaryResponse::from).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional
    public CreateAdminUserResponse createUser(CreateAdminUserRequest request) {
        UUID actorId = requireActor();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (profileRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email đã tồn tại trên hệ thống");
        }
        UserRole role = request.role() == null || request.role().isBlank()
                ? UserRole.USER
                : UserRole.valueOf(request.role().trim().toUpperCase(Locale.ROOT));
        String displayName = resolveDisplayName(request.displayName(), email);
        boolean sendEmail = request.sendCredentialsEmail() == null || request.sendCredentialsEmail();
        String tempPassword = randomPassword(12);

        Profile profile = Profile.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .displayName(displayName)
                .provider("local")
                .role(role.name())
                .tier(UserTier.FREE.name())
                .orgSubscription(OrgSubscription.NONE.name())
                .accountStatus(AccountStatus.ACTIVE.name())
                .emailVerified(true)
                .emailVerifiedAt(Instant.now())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build();
        Profile saved = profileRepository.save(profile);

        boolean emailed = false;
        if (sendEmail) {
            emailed = dispatchCredentialsEmail(email, displayName, tempPassword);
        }

        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                saved.getId(),
                "USER_CREATED",
                null,
                null,
                "Admin tạo / mời người dùng mới",
                null,
                Map.of("email", email, "role", role.name())));

        return new CreateAdminUserResponse(
                saved.getId(),
                email,
                displayName,
                role.name(),
                true,
                emailed,
                emailed ? null : tempPassword);
    }

    @Transactional
    public AdminUserSummaryResponse updateRole(UUID userId, UpdateUserRoleRequest request) {
        UUID actorId = requireActor();
        Profile profile = profileRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        UserRole currentRole = UserRole.fromStored(profile.getRole());
        UserRole expectedRole = UserRole.valueOf(request.expectedRole().trim().toUpperCase());
        UserRole nextRole = UserRole.valueOf(request.role().trim().toUpperCase());
        String reason = requireReason(request.reason());
        if (currentRole != expectedRole) {
            throw new ConflictException("Vai trò người dùng đã thay đổi; hãy tải lại trước khi thao tác");
        }
        assertRoleChangeAllowed(actorId, userId, currentRole, nextRole);
        if (currentRole == nextRole) {
            return AdminUserSummaryResponse.from(profile);
        }
        String previousRole = profile.getRole();
        profile.setRole(nextRole.name());
        Profile saved = profileRepository.save(profile);
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                userId,
                "ROLE_CHANGED",
                null,
                null,
                reason,
                null,
                Map.of("oldRole", previousRole == null ? "" : previousRole, "newRole", nextRole.name())));
        return AdminUserSummaryResponse.from(saved);
    }

    @Transactional
    public AdminUserSummaryResponse updateStatus(UUID userId, UpdateUserStatusRequest request) {
        UUID actorId = requireActor();
        Profile profile = profileRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        AccountStatus current = AccountStatus.fromStored(profile.getAccountStatus());
        AccountStatus expected = AccountStatus.valueOf(request.expectedStatus().trim().toUpperCase());
        AccountStatus next = AccountStatus.valueOf(request.status().trim().toUpperCase());
        String reason = requireReason(request.reason());
        if (current != expected) {
            throw new ConflictException("Trạng thái tài khoản đã thay đổi; hãy tải lại trước khi thao tác");
        }
        if (actorId.equals(userId) && next == AccountStatus.SUSPENDED) {
            throw new BusinessRuleException("Không thể khóa tạm thời chính mình");
        }
        if (next == AccountStatus.SUSPENDED
                && UserRole.fromStored(profile.getRole()) == UserRole.ADMIN
                && countActiveAdminsExcluding(userId) <= 0) {
            throw new BusinessRuleException("Không thể khóa ADMIN cuối cùng còn hoạt động");
        }
        if (current == next) {
            return AdminUserSummaryResponse.from(profile);
        }
        profile.setAccountStatus(next.name());
        Profile saved = profileRepository.save(profile);
        String action = next == AccountStatus.SUSPENDED ? "USER_SUSPENDED" : "USER_UNSUSPENDED";
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                userId,
                action,
                null,
                null,
                reason,
                null,
                Map.of("oldStatus", current.name(), "newStatus", next.name())));
        return AdminUserSummaryResponse.from(saved);
    }

    @Transactional
    public AdminUserSummaryResponse updateTier(UUID userId, UpdateUserTierRequest request) {
        UUID actorId = requireActor();
        Profile profile = profileRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        UserTier current = UserTier.fromStored(profile.getTier());
        UserTier expected = UserTier.valueOf(request.expectedTier().trim().toUpperCase());
        UserTier next = UserTier.valueOf(request.tier().trim().toUpperCase());
        String reason = requireReason(request.reason());
        if (current != expected) {
            throw new ConflictException("Gói người dùng đã thay đổi; hãy tải lại trước khi thao tác");
        }
        if (current == next) {
            return AdminUserSummaryResponse.from(profile);
        }
        profile.setTier(next.name());
        Profile saved = profileRepository.save(profile);
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                userId,
                "TIER_CHANGED",
                null,
                null,
                reason,
                null,
                Map.of("oldTier", current.name(), "newTier", next.name())));
        return AdminUserSummaryResponse.from(saved);
    }

    @Transactional
    public AdminUserSummaryResponse forceVerify(UUID userId, ForceVerifyRequest request) {
        UUID actorId = requireActor();
        String reason = requireReason(request.reason());
        Profile profile = profileRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (Boolean.TRUE.equals(profile.getEmailVerified())) {
            return AdminUserSummaryResponse.from(profile);
        }
        profile.setEmailVerified(true);
        profile.setEmailVerifiedAt(Instant.now());
        Profile saved = profileRepository.save(profile);
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                userId,
                "EMAIL_FORCE_VERIFIED",
                null,
                null,
                reason,
                null,
                Map.of("email", saved.getEmail() == null ? "" : saved.getEmail())));
        return AdminUserSummaryResponse.from(saved);
    }

    @Transactional
    public AdminResendVerificationResponse resendVerification(UUID userId) {
        UUID actorId = requireActor();
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        if (Boolean.TRUE.equals(profile.getEmailVerified())) {
            throw new BusinessRuleException("Email đã được xác minh");
        }
        String debugToken = emailVerificationService.sendAdminVerificationEmail(userId);
        boolean emailed = mailProperties.isEnabled() && debugToken == null;
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                userId,
                "EMAIL_VERIFICATION_RESENT",
                null,
                null,
                "Admin gửi lại email xác thực",
                null,
                Map.of("email", profile.getEmail() == null ? "" : profile.getEmail(), "emailed", emailed)));
        return new AdminResendVerificationResponse(userId, emailed, debugToken);
    }

    @Transactional
    public byte[] exportUsersCsv(String q, String role, String tier, Boolean verified, String status, String idsCsv) {
        UUID actorId = requireActor();
        List<Profile> rows;
        Map<String, Object> meta = new LinkedHashMap<>();
        if (idsCsv != null && !idsCsv.isBlank()) {
            List<UUID> ids = parseIds(idsCsv);
            if (ids.size() > EXPORT_MAX_ROWS) {
                throw new BusinessRuleException("Xuất tối đa " + EXPORT_MAX_ROWS + " người dùng");
            }
            rows = profileRepository.findByIdIn(ids);
            meta.put("mode", "ids");
            meta.put("requestedIds", ids.size());
        } else {
            rows = profileRepository.findAdminUsersForExport(
                    blankToEmpty(q),
                    upperOrEmpty(role),
                    upperOrEmpty(tier),
                    verified,
                    upperOrEmpty(status),
                    PageRequest.of(0, EXPORT_MAX_ROWS));
            meta.put("mode", "filter");
            meta.put("q", blankToEmpty(q));
            meta.put("role", upperOrEmpty(role));
            meta.put("tier", upperOrEmpty(tier));
            meta.put("status", upperOrEmpty(status));
            meta.put("verified", verified == null ? "" : String.valueOf(verified));
        }
        meta.put("count", rows.size());
        audit.append(new AdminAuditCommand(
                actorId,
                "USER",
                actorId,
                "USERS_EXPORTED",
                null,
                null,
                "Admin xuất CSV người dùng",
                null,
                meta));

        StringBuilder sb = new StringBuilder();
        sb.append("id,email,displayName,role,tier,emailVerified,accountStatus,level,totalPoints,createdAt\n");
        for (Profile p : rows) {
            sb.append(csv(p.getId() == null ? "" : p.getId().toString()))
                    .append(',')
                    .append(csv(p.getEmail()))
                    .append(',')
                    .append(csv(p.getDisplayName()))
                    .append(',')
                    .append(csv(UserRole.fromStored(p.getRole()).name()))
                    .append(',')
                    .append(csv(UserTier.fromStored(p.getTier()).name()))
                    .append(',')
                    .append(Boolean.TRUE.equals(p.getEmailVerified()) ? "true" : "false")
                    .append(',')
                    .append(csv(AccountStatus.fromStored(p.getAccountStatus()).name()))
                    .append(',')
                    .append(p.getLevel() == null ? "" : p.getLevel())
                    .append(',')
                    .append(p.getTotalPoints() == null ? "" : p.getTotalPoints())
                    .append(',')
                    .append(p.getCreatedAt() == null ? "" : p.getCreatedAt().toString())
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public AdminUserBulkResponse bulk(AdminUserBulkRequest request) {
        if (request.userIds().size() > BULK_MAX) {
            throw new BusinessRuleException("Bulk tối đa " + BULK_MAX + " người dùng mỗi lần");
        }
        String action = request.action().trim().toUpperCase(Locale.ROOT);
        List<AdminUserBulkResponse.Failure> failed = new ArrayList<>();
        int succeeded = 0;
        for (UUID userId : request.userIds()) {
            try {
                switch (action) {
                    case "SUSPEND" -> updateStatus(
                            userId,
                            new UpdateUserStatusRequest(
                                    "SUSPENDED",
                                    requireReason(request.reason()),
                                    AccountStatus.fromStored(loadStatus(userId)).name()));
                    case "UNSUSPEND" -> updateStatus(
                            userId,
                            new UpdateUserStatusRequest(
                                    "ACTIVE",
                                    requireReason(request.reason()),
                                    AccountStatus.fromStored(loadStatus(userId)).name()));
                    case "SET_ROLE" -> {
                        if (request.role() == null || request.role().isBlank()) {
                            throw new BusinessRuleException("role bắt buộc với SET_ROLE");
                        }
                        Profile p = profileRepository
                                .findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                        updateRole(
                                userId,
                                new UpdateUserRoleRequest(
                                        request.role(),
                                        requireReason(request.reason()),
                                        UserRole.fromStored(p.getRole()).name()));
                    }
                    case "SET_TIER" -> {
                        if (request.tier() == null || request.tier().isBlank()) {
                            throw new BusinessRuleException("tier bắt buộc với SET_TIER");
                        }
                        Profile p = profileRepository
                                .findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                        updateTier(
                                userId,
                                new UpdateUserTierRequest(
                                        request.tier(),
                                        requireReason(request.reason()),
                                        UserTier.fromStored(p.getTier()).name()));
                    }
                    default -> throw new BusinessRuleException("Action không hỗ trợ: " + action);
                }
                succeeded++;
            } catch (RuntimeException ex) {
                failed.add(new AdminUserBulkResponse.Failure(
                        userId, ex.getMessage() == null ? "Lỗi không xác định" : ex.getMessage()));
            }
        }
        return new AdminUserBulkResponse(succeeded, failed);
    }

    private String loadStatus(UUID userId) {
        return profileRepository
                .findById(userId)
                .map(Profile::getAccountStatus)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void assertRoleChangeAllowed(UUID actorId, UUID userId, UserRole currentRole, UserRole nextRole) {
        if (actorId.equals(userId) && currentRole == UserRole.ADMIN && nextRole != UserRole.ADMIN) {
            throw new BusinessRuleException("Không thể tự hạ quyền ADMIN của chính mình");
        }
        if (currentRole == UserRole.ADMIN
                && nextRole != UserRole.ADMIN
                && profileRepository.findAllAdminsForUpdate().size() <= 1) {
            throw new BusinessRuleException("Không thể hạ quyền ADMIN cuối cùng");
        }
    }

    private long countActiveAdminsExcluding(UUID excludeUserId) {
        return profileRepository.findAllAdminsForUpdate().stream()
                .filter(admin -> !admin.getId().equals(excludeUserId))
                .filter(admin -> !AccountStatus.fromStored(admin.getAccountStatus()).isSuspended())
                .count();
    }

    private UUID requireActor() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }

    private static String requireReason(String reason) {
        String trimmed = reason == null ? "" : reason.trim();
        if (trimmed.length() < 10) {
            throw new BusinessRuleException("Lý do cần ít nhất 10 ký tự");
        }
        return trimmed;
    }

    private boolean dispatchCredentialsEmail(String to, String displayName, String tempPassword) {
        String loginUrl = appProperties.getFrontendUrl().replaceAll("/$", "") + "/login";
        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — admin created user credentials for {}", to);
            return false;
        }
        try {
            String html =
                    """
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Quản trị HistAR đã tạo tài khoản cho bạn.</p>
                    <ul>
                      <li>Email đăng nhập: <strong>%s</strong></li>
                      <li>Mật khẩu tạm: <strong>%s</strong></li>
                    </ul>
                    <p><a href="%s">Đăng nhập HistAR</a></p>
                    """
                            .formatted(displayName, to, tempPassword, loginUrl);
            histarEmailService.sendHtml(to, "Tài khoản HistAR mới", html);
            return true;
        } catch (EmailDeliveryException ex) {
            throw new BusinessRuleException("Không gửi được email thông tin đăng nhập. Thử lại sau.");
        }
    }

    private static List<UUID> parseIds(String idsCsv) {
        String[] parts = idsCsv.split(",");
        List<UUID> ids = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(UUID.fromString(trimmed));
            } catch (IllegalArgumentException ex) {
                throw new BusinessRuleException("ids chứa UUID không hợp lệ: " + trimmed);
            }
        }
        if (ids.isEmpty()) {
            throw new BusinessRuleException("ids trống");
        }
        return ids;
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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

    private static String blankToEmpty(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String upperOrEmpty(String value) {
        String trimmed = blankToEmpty(value);
        return trimmed.isEmpty() ? "" : trimmed.toUpperCase(Locale.ROOT);
    }
}
