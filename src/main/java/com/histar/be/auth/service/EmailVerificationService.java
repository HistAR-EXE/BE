package com.histar.be.auth.service;

import com.histar.be.auth.entity.EmailVerificationToken;
import com.histar.be.auth.repository.EmailVerificationTokenRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final ProfileRepository profileRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final HistarAppProperties appProperties;
    private final HistarMailProperties mailProperties;
    private final TestHookProperties testHookProperties;

    private final Map<UUID, Instant> lastResendAt = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public boolean isVerified(UUID userId) {
        return profileRepository
                .findById(userId)
                .map(p -> Boolean.TRUE.equals(p.getEmailVerified()))
                .orElse(false);
    }

    @Async
    public void sendVerificationEmailAsync(UUID userId) {
        sendVerificationEmailAsync(userId, true);
    }

    /** First send after register — no 60s resend cooldown. */
    @Async
    public void sendInitialVerificationEmailAsync(UUID userId) {
        sendVerificationEmailAsync(userId, false);
    }

    private void sendVerificationEmailAsync(UUID userId, boolean enforceCooldown) {
        try {
            sendVerificationEmail(userId, enforceCooldown);
        } catch (Exception ex) {
            log.warn("Failed to send verification email for user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    @Transactional
    public String sendVerificationEmail(UUID userId) {
        return sendVerificationEmail(userId, true);
    }

    /** Initial verification mail (register) — skips resend cooldown. */
    @Transactional
    public String sendInitialVerificationEmail(UUID userId) {
        return sendVerificationEmail(userId, false);
    }

    @Transactional
    public String sendVerificationEmail(UUID userId, boolean enforceCooldown) {
        Profile profile = profileRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessRuleException("User not found"));
        if (Boolean.TRUE.equals(profile.getEmailVerified())) {
            return null;
        }
        if (enforceCooldown) {
            enforceResendCooldown(userId);
        }

        String rawToken = randomToken();
        Instant now = Instant.now();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(userId)
                .tokenHash(hashToken(rawToken))
                .expiresAt(now.plus(mailProperties.getVerificationTtlHours(), ChronoUnit.HOURS))
                .createdAt(now)
                .build());

        dispatchEmail(profile.getEmail(), profile.getDisplayName(), rawToken);
        lastResendAt.put(userId, Instant.now());
        // Test hooks: always return raw token so E2E can verify without SMTP (MAIL_ENABLED=true OK).
        if (testHookProperties.isEnabled() || !mailProperties.isEnabled()) {
            return rawToken;
        }
        return null;
    }

    @Transactional
    public VerifyResult confirmToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new BusinessRuleException("Token xác thực không hợp lệ");
        }
        EmailVerificationToken stored = tokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(rawToken.trim()))
                .orElseThrow(() -> new BusinessRuleException("Link xác thực không hợp lệ hoặc đã được sử dụng"));
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleException("Link xác thực đã hết hạn. Vui lòng gửi lại email.");
        }

        Profile profile = profileRepository
                .findById(stored.getUserId())
                .orElseThrow(() -> new BusinessRuleException("User not found"));
        profile.setEmailVerified(true);
        profile.setEmailVerifiedAt(Instant.now());
        profileRepository.save(profile);

        stored.setUsedAt(Instant.now());
        tokenRepository.save(stored);

        return new VerifyResult(true, profile.getEmail());
    }

    @Transactional
    public void markVerifiedFromOAuth(UUID userId) {
        profileRepository.findById(userId).ifPresent(profile -> {
            if (!Boolean.TRUE.equals(profile.getEmailVerified())) {
                profile.setEmailVerified(true);
                profile.setEmailVerifiedAt(Instant.now());
                profileRepository.save(profile);
            }
        });
    }

    private void enforceResendCooldown(UUID userId) {
        Instant last = lastResendAt.get(userId);
        if (last == null) {
            return;
        }
        Instant allowed = last.plus(mailProperties.getResendCooldownSeconds(), ChronoUnit.SECONDS);
        if (Instant.now().isBefore(allowed)) {
            throw new BusinessRuleException("Vui lòng đợi "
                    + mailProperties.getResendCooldownSeconds()
                    + " giây trước khi gửi lại email xác thực.");
        }
    }

    private void dispatchEmail(String to, String displayName, String rawToken) {
        if (!mailProperties.isEnabled()) {
            String link = buildVerifyLink(rawToken);
            log.info("Mail disabled — verification link for {}: {}", to, link);
            return;
        }
        // Avoid SMTP cooldown / delivery failures in local/E2E when test hooks are on.
        if (testHookProperties.isEnabled()) {
            String link = buildVerifyLink(rawToken);
            log.info("Test hooks enabled — skip SMTP; verification link for {}: {}", to, link);
            return;
        }
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to);
            helper.setSubject("Xác thực email TimeLens");
            String link = buildVerifyLink(rawToken);
            String name = displayName != null && !displayName.isBlank() ? displayName : "bạn";
            helper.setText(
                    """
                    <p>Xin chào %s,</p>
                    <p>Nhấn vào liên kết bên dưới để xác thực email và bắt đầu dùng TimeLens (gói Free). Khi cần, bạn vẫn có thể nâng cấp Premium hoặc gói trường học sau:</p>
                    <p><a href="%s">Xác thực email TimeLens</a></p>
                    <p>Liên kết hết hạn sau %d giờ. Nếu bạn không đăng ký TimeLens, hãy bỏ qua email này.</p>
                    """
                            .formatted(name, link, mailProperties.getVerificationTtlHours()),
                    true);
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("SMTP failed sending verification email to {}: {}", to, ex.getMessage(), ex);
            throw new BusinessRuleException("Không gửi được email xác thực. Thử lại sau.");
        }
    }

    private String buildVerifyLink(String rawToken) {
        String base = appProperties.getFrontendUrl().replaceAll("/$", "");
        return base + "/verify-email?token=" + rawToken;
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public record VerifyResult(boolean verified, String email) {}
}
