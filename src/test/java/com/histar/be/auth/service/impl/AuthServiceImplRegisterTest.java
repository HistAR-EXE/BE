package com.histar.be.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.auth.entity.RefreshToken;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.auth.service.EmailVerificationService;
import com.histar.be.auth.service.FirebaseAuthService;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.service.ProfileService;
import com.histar.be.referral.service.ReferralService;
import com.histar.be.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplRegisterTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private FirebaseAuthService firebaseAuthService;

    @Mock
    private HistarMailProperties mailProperties;

    @Mock
    private ReferralService referralService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Profile unverifiedProfile;

    @BeforeEach
    void setUp() {
        unverifiedProfile = Profile.builder()
                .id(UUID.randomUUID())
                .email("new@histar.vn")
                .displayName("New User")
                .role(UserRole.USER.name())
                .tier(UserTier.FREE.name())
                .orgSubscription(OrgSubscription.NONE.name())
                .emailVerified(false)
                .createdAt(Instant.now())
                .build();
        when(jwtService.generateAccessToken(any(), any(), any(), any(), any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessExpiration()).thenReturn(3600_000L);
        when(jwtService.getRefreshExpiration()).thenReturn(7_200_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void register_sendsEmailAndReturnsUnverified() {
        when(profileService.findByEmail("new@histar.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(profileService.save(any(Profile.class))).thenAnswer(inv -> {
            Profile p = inv.getArgument(0);
            p.setId(unverifiedProfile.getId());
            return p;
        });
        when(emailVerificationService.sendInitialVerificationEmail(any(UUID.class))).thenReturn("debug-token");
        when(mailProperties.isEnabled()).thenReturn(false);

        AuthResponse response = authService.register(new RegisterRequest("new@histar.vn", "pass123", "New User"));

        assertThat(response.emailVerified()).isFalse();
        assertThat(response.email()).isEqualTo("new@histar.vn");
        assertThat(response.debugVerificationToken()).isEqualTo("debug-token");
        verify(emailVerificationService).sendInitialVerificationEmail(any(UUID.class));
    }

    @Test
    void register_mailEnabled_sendsAsyncAndReturnsWithoutBlocking() {
        when(profileService.findByEmail("new@histar.vn")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(profileService.save(any(Profile.class))).thenAnswer(inv -> {
            Profile p = inv.getArgument(0);
            p.setId(unverifiedProfile.getId());
            return p;
        });
        when(mailProperties.isEnabled()).thenReturn(true);

        AuthResponse response = authService.register(new RegisterRequest("new@histar.vn", "pass123", "New User"));

        assertThat(response.emailVerified()).isFalse();
        assertThat(response.email()).isEqualTo("new@histar.vn");
        assertThat(response.debugVerificationToken()).isNull();
        verify(emailVerificationService).sendInitialVerificationEmailAsync(any(UUID.class));
    }

    @Test
    void login_unverifiedReturnsEmailVerifiedFalse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(profileService.findByEmail("new@histar.vn")).thenReturn(Optional.of(unverifiedProfile));

        AuthResponse response = authService.login(new LoginRequest("new@histar.vn", "pass123"));

        assertThat(response.emailVerified()).isFalse();
    }
}
