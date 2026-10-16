package com.histar.be.admin.service;

import com.histar.be.admin.dto.AdminUserSummaryResponse;
import com.histar.be.admin.dto.CreateAdminUserRequest;
import com.histar.be.admin.dto.CreateAdminUserResponse;
import com.histar.be.admin.dto.UpdateUserRoleRequest;
import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
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
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.security.SecureRandom;
import java.time.Instant;
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

    private final ProfileRepository profileRepository;
    private final CurrentUserAccessor currentUserAccessor;
    private final AdminAuditService audit;
    private final PasswordEncoder passwordEncoder;
    private final HistarEmailService histarEmailService;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;

    public PageResponse<AdminUserSummaryResponse> listUsers(int page, int size) {
        return listUsers(page, size, null, null, null, null);
    }

    public PageResponse<AdminUserSummaryResponse> listUsers(
            int page, int size, String q, String role, String tier, Boolean verified) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        // Empty strings (not null) keep PostgreSQL from binding LIKE params as bytea.
        String normalizedQ = blankToEmpty(q);
        String normalizedRole = upperOrEmpty(role);
        String normalizedTier = upperOrEmpty(tier);
        Page<Profile> result = profileRepository.findAdminUsers(
                normalizedQ,
                normalizedRole,
                normalizedTier,
                verified,
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
        UUID actorId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
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
        UUID actorId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        Profile profile = profileRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        UserRole currentRole = UserRole.fromStored(profile.getRole());
        UserRole expectedRole = UserRole.valueOf(request.expectedRole().trim().toUpperCase());
        UserRole nextRole = UserRole.valueOf(request.role().trim().toUpperCase());
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.length() < 10) {
            throw new BusinessRuleException("Lý do thay đổi vai trò cần ít nhất 10 ký tự");
        }
        if (currentRole != expectedRole) {
            throw new ConflictException("Vai trò người dùng đã thay đổi; hãy tải lại trước khi thao tác");
        }
        if (actorId.equals(userId)
                && currentRole == UserRole.ADMIN
                && nextRole != UserRole.ADMIN) {
            throw new BusinessRuleException("Không thể tự hạ quyền ADMIN của chính mình");
        }
        if (currentRole == UserRole.ADMIN
                && nextRole != UserRole.ADMIN
                && profileRepository.findAllAdminsForUpdate().size() <= 1) {
            throw new BusinessRuleException("Không thể hạ quyền ADMIN cuối cùng");
        }
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
