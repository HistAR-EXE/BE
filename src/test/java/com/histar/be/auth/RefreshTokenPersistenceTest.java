package com.histar.be.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.LogoutRequest;
import com.histar.be.auth.dto.RefreshTokenRequest;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.auth.service.AuthService;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.repository.ProfileRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RefreshTokenPersistenceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void refreshRotatesPersistedToken() {
        profileRepository.save(Profile.builder()
                .email("refresh@test.local")
                .passwordHash(passwordEncoder.encode("Test1234!"))
                .displayName("Refresh User")
                .provider("local")
                .role(UserRole.USER.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build());

        var login = authService.login(new LoginRequest("refresh@test.local", "Test1234!"));
        assertEquals(1, refreshTokenRepository.count());

        var refreshed = authService.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertNotEquals(login.refreshToken(), refreshed.refreshToken());
        assertTrue(refreshTokenRepository.findByTokenAndRevokedFalse(refreshed.refreshToken()).isPresent());

        authService.logout(new LogoutRequest(refreshed.refreshToken()));
        assertTrue(refreshTokenRepository
                .findByTokenAndRevokedFalse(refreshed.refreshToken())
                .isEmpty());
    }
}
