package com.histar.be.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.entity.EmailVerificationToken;
import com.histar.be.auth.repository.EmailVerificationTokenRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarAppProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private HistarAppProperties appProperties;

    @Mock
    private HistarMailProperties mailProperties;

    @Mock
    private TestHookProperties testHookProperties;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    private UUID userId;
    private Profile unverified;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        unverified = Profile.builder()
                .id(userId)
                .email("user@histar.vn")
                .displayName("Tester")
                .emailVerified(false)
                .build();
        lenient().when(mailProperties.getVerificationTtlHours()).thenReturn(24);
        lenient().when(mailProperties.getResendCooldownSeconds()).thenReturn(60);
        lenient().when(appProperties.getFrontendUrl()).thenReturn("http://localhost:5173");
        lenient().when(testHookProperties.isEnabled()).thenReturn(false);
    }

    @Test
    void AUTH_EV_BE10_mailDisabled_returnsDebugTokenAndDoesNotSend() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(false);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        String token = emailVerificationService.sendVerificationEmail(userId);

        assertThat(token).isNotBlank();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void AUTH_EV_BE11_mailEnabled_sendsSmtpAndReturnsNull() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(true);
        when(testHookProperties.isEnabled()).thenReturn(false);
        when(mailProperties.getFrom()).thenReturn("noreply@histar.vn");
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);

        String token = emailVerificationService.sendVerificationEmail(userId);

        assertThat(token).isNull();
        verify(mailSender).send(mime);
    }

    @Test
    void AUTH_EV_BE11b_mailEnabledButTestHooks_skipsSmtpAndReturnsDebugToken() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(true);
        when(testHookProperties.isEnabled()).thenReturn(true);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        String token = emailVerificationService.sendVerificationEmail(userId);

        assertThat(token).isNotBlank();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void AUTH_EV_BE12_confirmOnce_marksVerified() {
        String raw = "abc123token";
        EmailVerificationToken stored = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(EmailVerificationService.hashToken(raw))
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .createdAt(Instant.now())
                .build();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(EmailVerificationService.hashToken(raw)))
                .thenReturn(Optional.of(stored));
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        var result = emailVerificationService.confirmToken(raw);

        assertThat(result.verified()).isTrue();
        assertThat(unverified.getEmailVerified()).isTrue();
        assertThat(stored.getUsedAt()).isNotNull();
    }

    @Test
    void AUTH_EV_BE13_confirmTwice_throwsUsed() {
        String raw = "used-token";
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(EmailVerificationService.hashToken(raw)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> emailVerificationService.confirmToken(raw))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không hợp lệ hoặc đã được sử dụng");
    }

    @Test
    void AUTH_EV_BE14_expiredToken_throws() {
        String raw = "expired-token";
        EmailVerificationToken stored = EmailVerificationToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(EmailVerificationService.hashToken(raw))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();
        when(tokenRepository.findByTokenHashAndUsedAtIsNull(EmailVerificationService.hashToken(raw)))
                .thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> emailVerificationService.confirmToken(raw))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void AUTH_EV_BE15_resendCooldown_throws() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(false);
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        emailVerificationService.sendVerificationEmail(userId);

        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đợi");
    }

    @Test
    void AUTH_EV_BE16_smtpFailure_doesNotStartResendCooldown() throws Exception {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(true);
        when(testHookProperties.isEnabled()).thenReturn(false);
        when(mailProperties.getFrom()).thenReturn("noreply@histar.vn");
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP auth failed"))
                .when(mailSender)
                .send(any(MimeMessage.class));

        assertThatThrownBy(() -> emailVerificationService.sendVerificationEmail(userId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không gửi được email");

        when(mailProperties.isEnabled()).thenReturn(false);
        String token = emailVerificationService.sendVerificationEmail(userId);
        assertThat(token).isNotBlank();
    }

    @Test
    void sendVerificationEmail_persistsHashedToken() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(unverified));
        when(mailProperties.isEnabled()).thenReturn(false);
        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        when(tokenRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        String raw = emailVerificationService.sendVerificationEmail(userId);

        assertThat(captor.getValue().getTokenHash()).isEqualTo(EmailVerificationService.hashToken(raw));
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
    }
}
