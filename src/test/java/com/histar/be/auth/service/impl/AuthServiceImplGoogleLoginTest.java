package com.histar.be.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.GoogleLoginRequest;
import com.histar.be.auth.entity.RefreshToken;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.auth.service.EmailVerificationService;
import com.histar.be.auth.service.FirebaseAuthService;
import com.histar.be.auth.service.FirebaseAuthService.VerifiedGoogleUser;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.service.ProfileService;
import com.histar.be.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplGoogleLoginTest {

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

    @InjectMocks
    private AuthServiceImpl authService;

    private VerifiedGoogleUser googleUser;

    @BeforeEach
    void setUp() {
        googleUser = new VerifiedGoogleUser("firebase-uid-1", "google.user@histar.vn", "Google User", "https://pic.test/a.png");
        when(jwtService.generateAccessToken(any(), anyString(), anyString(), any(), anyString()))
                .thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh-token");
        when(jwtService.getAccessExpiration()).thenReturn(3600_000L);
        when(jwtService.getRefreshExpiration()).thenReturn(7_200_000L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void googleLogin_createsNewProfileWithEmailVerified() {
        when(firebaseAuthService.verifyIdToken("valid-token")).thenReturn(googleUser);
        when(profileService.findByEmail(googleUser.email())).thenReturn(Optional.empty());
        when(profileService.save(any(Profile.class))).thenAnswer(inv -> {
            Profile p = inv.getArgument(0);
            p.setId(UUID.randomUUID());
            return p;
        });

        AuthResponse response = authService.googleLogin(new GoogleLoginRequest("valid-token"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.emailVerified()).isTrue();
        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileService).save(captor.capture());
        Profile saved = captor.getValue();
        assertThat(saved.getProvider()).isEqualTo("google");
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(saved.getFirebaseUid()).isEqualTo("firebase-uid-1");
        assertThat(saved.getTier()).isEqualTo(UserTier.FREE.name());
    }

    @Test
    void googleLogin_linksLocalAccountAndVerifiesEmail() {
        UUID userId = UUID.randomUUID();
        Profile existing = Profile.builder()
                .id(userId)
                .email(googleUser.email())
                .provider("local")
                .emailVerified(false)
                .role(UserRole.USER.name())
                .tier(UserTier.FREE.name())
                .orgSubscription(OrgSubscription.NONE.name())
                .createdAt(Instant.now())
                .build();

        when(firebaseAuthService.verifyIdToken("valid-token")).thenReturn(googleUser);
        when(profileService.findByEmail(googleUser.email())).thenReturn(Optional.of(existing));
        when(profileService.save(any(Profile.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.googleLogin(new GoogleLoginRequest("valid-token"));

        ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
        verify(profileService).save(captor.capture());
        Profile saved = captor.getValue();
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(saved.getEmailVerifiedAt()).isNotNull();
        assertThat(saved.getFirebaseUid()).isEqualTo("firebase-uid-1");
    }
}
