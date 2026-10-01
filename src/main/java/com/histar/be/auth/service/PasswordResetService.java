package com.histar.be.auth.service;

import com.histar.be.auth.dto.PasswordResetRequestResponse;
import com.histar.be.auth.dto.PasswordResetVerifyResponse;
import com.histar.be.auth.entity.PasswordResetChallenge;
import com.histar.be.auth.entity.RefreshToken;
import com.histar.be.auth.repository.PasswordResetChallengeRepository;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.mail.EmailDeliveryException;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final String GENERIC_REQUEST_MESSAGE =
            "Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP đặt lại mật khẩu.";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final ProfileService profileService;
    private final PasswordResetChallengeRepository challengeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HistarEmailService histarEmailService;
    private final HistarMailProperties mailProperties;
    private final TestHookProperties testHookProperties;
    private final PasswordEncoder passwordEncoder;

    private final Map<UUID, Instant> lastRequestAt = new ConcurrentHashMap<>();

    @Transactional
    public PasswordResetRequestResponse requestReset(String email) {
        String normalized = email == null ? "" : email.trim();
        Optional<Profile> profileOpt = profileService.findByEmail(normalized);
        if (profileOpt.isEmpty()) {
            return new PasswordResetRequestResponse(GENERIC_REQUEST_MESSAGE);
        }

        Profile profile = profileOpt.get();
        if (profile.getPasswordHash() == null || profile.getPasswordHash().isBlank()) {
            return new PasswordResetRequestResponse(GENERIC_REQUEST_MESSAGE);
        }

        enforceRequestCooldown(profile.getId());

        String otp = randomOtp();
        Instant now = Instant.now();
        int ttlMinutes = Math.max(1, mailProperties.getPasswordResetOtpTtlMinutes());

        // Invalidate prior open challenges for this user.
        List<PasswordResetChallenge> open = challengeRepository.findByUserIdAndConsumedAtIsNull(profile.getId());
        for (PasswordResetChallenge prior : open) {
            prior.setConsumedAt(now);
        }
        challengeRepository.saveAll(open);

        challengeRepository.save(PasswordResetChallenge.builder()
                .userId(profile.getId())
                .otpHash(hashToken(otp))
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .attempts(0)
                .createdAt(now)
                .build());

        String debugOtp = dispatchOtpEmail(profile, otp, ttlMinutes);
        lastRequestAt.put(profile.getId(), now);
        return new PasswordResetRequestResponse(GENERIC_REQUEST_MESSAGE, debugOtp);
    }

    @Transactional
    public PasswordResetVerifyResponse verifyOtp(String email, String code) {
        String normalized = email == null ? "" : email.trim();
        Profile profile = profileService
                .findByEmail(normalized)
                .orElseThrow(() -> new BusinessRuleException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        PasswordResetChallenge challenge = challengeRepository
                .findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(profile.getId())
                .orElseThrow(() -> new BusinessRuleException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        Instant now = Instant.now();
        if (challenge.getVerifiedAt() != null) {
            throw new BusinessRuleException("OTP đã được xác thực. Vui lòng đặt mật khẩu mới.");
        }
        if (challenge.getExpiresAt().isBefore(now)) {
            throw new BusinessRuleException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }

        int maxAttempts = Math.max(1, mailProperties.getPasswordResetMaxAttempts());
        if (challenge.getAttempts() >= maxAttempts) {
            throw new BusinessRuleException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng gửi lại mã mới.");
        }

        String trimmedCode = code == null ? "" : code.trim();
        if (!hashToken(trimmedCode).equals(challenge.getOtpHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            if (challenge.getAttempts() >= maxAttempts) {
                throw new BusinessRuleException("Bạn đã nhập sai OTP quá nhiều lần. Vui lòng gửi lại mã mới.");
            }
            throw new BusinessRuleException("Mã OTP không đúng.");
        }

        String resetToken = randomResetToken();
        int tokenTtlMinutes = Math.max(1, mailProperties.getPasswordResetTokenTtlMinutes());
        challenge.setVerifiedAt(now);
        challenge.setResetTokenHash(hashToken(resetToken));
        challenge.setResetTokenExpiresAt(now.plus(tokenTtlMinutes, ChronoUnit.MINUTES));
        challengeRepository.save(challenge);

        return new PasswordResetVerifyResponse("OTP hợp lệ. Vui lòng đặt mật khẩu mới.", resetToken);
    }

    @Transactional
    public void confirmReset(String resetToken, String newPassword) {
        if (resetToken == null || resetToken.isBlank()) {
            throw new BusinessRuleException("Token đặt lại mật khẩu không hợp lệ.");
        }

        PasswordResetChallenge challenge = challengeRepository
                .findByResetTokenHashAndConsumedAtIsNull(hashToken(resetToken.trim()))
                .orElseThrow(() -> new BusinessRuleException("Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."));

        Instant now = Instant.now();
        if (challenge.getVerifiedAt() == null) {
            throw new BusinessRuleException("Token đặt lại mật khẩu chưa được xác thực OTP.");
        }
        if (challenge.getResetTokenExpiresAt() == null || challenge.getResetTokenExpiresAt().isBefore(now)) {
            throw new BusinessRuleException("Token đặt lại mật khẩu đã hết hạn. Vui lòng gửi lại OTP.");
        }

        Profile profile = profileService.findById(challenge.getUserId());
        profile.setPasswordHash(passwordEncoder.encode(newPassword));
        profileService.save(profile);

        challenge.setConsumedAt(now);
        challengeRepository.save(challenge);

        // Consume any other open challenges for safety.
        List<PasswordResetChallenge> open = challengeRepository.findByUserIdAndConsumedAtIsNull(profile.getId());
        for (PasswordResetChallenge prior : open) {
            prior.setConsumedAt(now);
        }
        challengeRepository.saveAll(open);

        revokeRefreshTokens(profile.getId());
    }

    private void revokeRefreshTokens(UUID userId) {
        List<RefreshToken> active = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : active) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(active);
    }

    private void enforceRequestCooldown(UUID userId) {
        Instant last = lastRequestAt.get(userId);
        if (last == null) {
            return;
        }
        int cooldown = Math.max(1, mailProperties.getResendCooldownSeconds());
        Instant allowed = last.plus(cooldown, ChronoUnit.SECONDS);
        if (Instant.now().isBefore(allowed)) {
            throw new BusinessRuleException("Vui lòng đợi " + cooldown + " giây trước khi gửi lại OTP.");
        }
    }

    private String dispatchOtpEmail(Profile profile, String otp, int ttlMinutes) {
        String to = profile.getEmail();
        if (!mailProperties.isEnabled()) {
            log.info("Mail disabled — password reset OTP for {}: {}", to, otp);
            return otp;
        }
        if (testHookProperties.isEnabled()) {
            log.info("Test hooks enabled — skip mail; password reset OTP for {}: {}", to, otp);
            return otp;
        }

        String name = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                ? profile.getDisplayName()
                : "bạn";
        String html =
                """
                <p>Xin chào %s,</p>
                <p>Mã OTP đặt lại mật khẩu TimeLens của bạn là:</p>
                <p style="font-size:24px;font-weight:bold;letter-spacing:4px;">%s</p>
                <p>Mã hết hạn sau %d phút. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.</p>
                """
                        .formatted(name, otp, ttlMinutes);
        try {
            histarEmailService.sendHtml(to, "Mã OTP đặt lại mật khẩu TimeLens", html);
        } catch (EmailDeliveryException ex) {
            throw new BusinessRuleException("Không gửi được email OTP. Thử lại sau.");
        }
        return null;
    }

    private static String randomOtp() {
        int value = RANDOM.nextInt(1_000_000);
        return String.format("%06d", value);
    }

    private static String randomResetToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HEX.formatHex(bytes);
    }

    static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
