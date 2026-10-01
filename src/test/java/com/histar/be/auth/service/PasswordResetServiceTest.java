package com.histar.be.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.dto.PasswordResetRequestResponse;
import com.histar.be.auth.dto.PasswordResetVerifyResponse;
import com.histar.be.auth.entity.PasswordResetChallenge;
import com.histar.be.auth.entity.RefreshToken;
import com.histar.be.auth.repository.PasswordResetChallengeRepository;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import com.histar.be.mail.HistarEmailService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private PasswordResetChallengeRepository challengeRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private HistarEmailService histarEmailService;

    @Mock
    private HistarMailProperties mailProperties;

    @Mock
    private TestHookProperties testHookProperties;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private UUID userId;
    private Profile localUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        localUser = Profile.builder()
                .id(userId)
                .email("local@histar.vn")
                .displayName("Local User")
                .passwordHash("$2a$hashed")
                .provider("local")
                .build();
        lenient().when(mailProperties.getPasswordResetOtpTtlMinutes()).thenReturn(10);
        lenient().when(mailProperties.getPasswordResetMaxAttempts()).thenReturn(5);
        lenient().when(mailProperties.getPasswordResetTokenTtlMinutes()).thenReturn(15);
        lenient().when(mailProperties.getResendCooldownSeconds()).thenReturn(60);
        lenient().when(testHookProperties.isEnabled()).thenReturn(false);
    }

    @Test
    void requestReset_unknownEmail_returnsGenericMessageWithoutPersisting() {
        when(profileService.findByEmail("missing@histar.vn")).thenReturn(Optional.empty());

        PasswordResetRequestResponse response = passwordResetService.requestReset("missing@histar.vn");

        assertThat(response.message()).contains("Nếu email tồn tại");
        assertThat(response.debugOtp()).isNull();
        verify(challengeRepository, never()).save(any());
        verify(histarEmailService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void requestReset_googleOnly_returnsGenericMessageWithoutSending() {
        Profile google = Profile.builder()
                .id(userId)
                .email("google@histar.vn")
                .passwordHash(null)
                .provider("google")
                .build();
        when(profileService.findByEmail("google@histar.vn")).thenReturn(Optional.of(google));

        PasswordResetRequestResponse response = passwordResetService.requestReset("google@histar.vn");

        assertThat(response.message()).contains("Nếu email tồn tại");
        verify(challengeRepository, never()).save(any());
        verify(histarEmailService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void requestReset_localUser_mailDisabled_returnsDebugOtp() {
        when(profileService.findByEmail("local@histar.vn")).thenReturn(Optional.of(localUser));
        when(challengeRepository.findByUserIdAndConsumedAtIsNull(userId)).thenReturn(List.of());
        when(mailProperties.isEnabled()).thenReturn(false);

        PasswordResetRequestResponse response = passwordResetService.requestReset("local@histar.vn");

        assertThat(response.debugOtp()).matches("\\d{6}");
        ArgumentCaptor<PasswordResetChallenge> captor = ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challengeRepository).save(captor.capture());
        assertThat(captor.getValue().getOtpHash())
                .isEqualTo(PasswordResetService.hashToken(response.debugOtp()));
        verify(histarEmailService, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void verifyOtp_wrongCode_incrementsAttempts() {
        when(profileService.findByEmail("local@histar.vn")).thenReturn(Optional.of(localUser));
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .otpHash(PasswordResetService.hashToken("123456"))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .attempts(0)
                .createdAt(Instant.now())
                .build();
        when(challengeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> passwordResetService.verifyOtp("local@histar.vn", "000000"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không đúng");
        assertThat(challenge.getAttempts()).isEqualTo(1);
        verify(challengeRepository).save(challenge);
    }

    @Test
    void verifyOtp_expired_throws() {
        when(profileService.findByEmail("local@histar.vn")).thenReturn(Optional.of(localUser));
        PasswordResetChallenge challenge = PasswordResetChallenge.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .otpHash(PasswordResetService.hashToken("123456"))
                .expiresAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .attempts(0)
                .createdAt(Instant.now().minus(15, ChronoUnit.MINUTES))
                .build();
        when(challengeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> passwordResetService.verifyOtp("local@histar.vn", "123456"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void happyPath_verifyThenConfirm_updatesPasswordAndRevokesRefresh() {
        when(profileService.findByEmail("local@histar.vn")).thenReturn(Optional.of(localUser));
        when(challengeRepository.findByUserIdAndConsumedAtIsNull(userId)).thenReturn(List.of());
        when(mailProperties.isEnabled()).thenReturn(false);

        PasswordResetRequestResponse request = passwordResetService.requestReset("local@histar.vn");
        ArgumentCaptor<PasswordResetChallenge> saved = ArgumentCaptor.forClass(PasswordResetChallenge.class);
        verify(challengeRepository).save(saved.capture());
        PasswordResetChallenge challenge = saved.getValue();

        when(challengeRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(challenge));

        PasswordResetVerifyResponse verified =
                passwordResetService.verifyOtp("local@histar.vn", request.debugOtp());
        assertThat(verified.resetToken()).isNotBlank();

        when(challengeRepository.findByResetTokenHashAndConsumedAtIsNull(
                        PasswordResetService.hashToken(verified.resetToken())))
                .thenReturn(Optional.of(challenge));
        when(profileService.findById(userId)).thenReturn(localUser);
        when(passwordEncoder.encode("newpass1")).thenReturn("$2a$new");
        RefreshToken refresh = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("refresh-raw")
                .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .revoked(false)
                .createdAt(Instant.now())
                .build();
        when(refreshTokenRepository.findByUserIdAndRevokedFalse(userId)).thenReturn(List.of(refresh));

        passwordResetService.confirmReset(verified.resetToken(), "newpass1");

        assertThat(localUser.getPasswordHash()).isEqualTo("$2a$new");
        assertThat(challenge.getConsumedAt()).isNotNull();
        assertThat(refresh.isRevoked()).isTrue();
        verify(profileService).save(localUser);
        verify(refreshTokenRepository).saveAll(eq(List.of(refresh)));
    }
}
