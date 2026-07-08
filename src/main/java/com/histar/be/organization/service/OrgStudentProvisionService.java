package com.histar.be.organization.service;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.organization.dto.ProvisionStudentRequest;
import com.histar.be.organization.dto.ProvisionStudentResponse;
import com.histar.be.organization.entity.Organization;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgStudentProvisionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private final OrgAccessService orgAccessService;
    private final OrgMembershipService orgMembershipService;
    private final OrganizationRepository organizationRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;

    @Transactional
    public ProvisionStudentResponse provisionStudent(UUID orgId, UUID teacherUserId, ProvisionStudentRequest request) {
        orgAccessService.requireOrgAccess(orgId);
        Organization org = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        Profile teacher = profileRepository
                .findById(teacherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String email = request.email().trim().toLowerCase();
        String displayName = resolveDisplayName(request.displayName(), email);
        boolean sendEmail = request.sendCredentialsEmail() == null || request.sendCredentialsEmail();

        Optional<Profile> existing = profileRepository.findByEmail(email);
        if (existing.isPresent()) {
            Profile profile = existing.get();
            if (profile.getOrgId() != null && !profile.getOrgId().equals(orgId)) {
                throw new BusinessRuleException(
                        "Email đã thuộc tổ chức khác. Học sinh cần rời tổ chức hiện tại trước.");
            }
            orgMembershipService.enrollUserInOrganization(profile.getId(), orgId);
            return new ProvisionStudentResponse(
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
            dispatchCredentialsEmail(teacher.getDisplayName(), org.getName(), email, displayName, tempPassword);
            emailed = true;
        }

        return new ProvisionStudentResponse(
                profile.getId(),
                email,
                displayName,
                true,
                emailed,
                emailed ? null : tempPassword);
    }

    private void dispatchCredentialsEmail(
            String teacherName, String orgName, String to, String studentName, String tempPassword) {
        String loginUrl = appProperties.getFrontendUrl().replaceAll("/$", "") + "/login";
        if (!mailProperties.isEnabled()) {
            log.info(
                    "Mail disabled — student credentials for {} (org {}): temp password logged for teacher only",
                    to,
                    orgName);
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject("Tài khoản TimeLens — lớp " + orgName);
            helper.setText(
                    """
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Giáo viên <strong>%s</strong> đã tạo tài khoản TimeLens cho bạn tham gia lớp <strong>%s</strong>.</p>
                    <ul>
                      <li>Email đăng nhập: <strong>%s</strong></li>
                      <li>Mật khẩu tạm: <strong>%s</strong></li>
                    </ul>
                    <p><a href="%s">Đăng nhập TimeLens</a></p>
                    <p>Sau khi đăng nhập, hãy đổi mật khẩu tại Cài đặt → Hồ sơ. Bạn được hưởng quyền lợi Premium qua gói trường.</p>
                    """
                            .formatted(studentName, teacherName, orgName, to, tempPassword, loginUrl),
                    true);
            mailSender.send(message);
        } catch (Exception ex) {
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
}
