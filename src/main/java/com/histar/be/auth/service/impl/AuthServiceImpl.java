package com.histar.be.auth.service.impl;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.LogoutRequest;
import com.histar.be.auth.dto.RefreshTokenRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.common.exception.AuthException;
import com.histar.be.auth.service.AuthService;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import com.histar.be.security.JwtService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final Map<String, RefreshSession> refreshSessions = new ConcurrentHashMap<>();

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (profileService.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email đã được đăng ký");
        }
        Profile profile = Profile.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .provider("local")
                .role("USER")
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build();
        profileService.save(profile);
        return issueTokens(profile);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Profile profile = profileService
                .findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
        return issueTokens(profile);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshSession session = refreshSessions.get(request.refreshToken());
        if (session == null || session.expiresAt().isBefore(Instant.now()) || !jwtService.isValid(request.refreshToken())) {
            throw new AuthException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        Profile profile = profileService.findById(session.userId());
        refreshSessions.remove(request.refreshToken());
        return issueTokens(profile);
    }

    @Override
    public void logout(LogoutRequest request) {
        refreshSessions.remove(request.refreshToken());
    }

    private AuthResponse issueTokens(Profile profile) {
        String accessToken = jwtService.generateToken(profile.getEmail());
        String refreshToken = jwtService.generateRefreshToken(profile.getEmail());
        refreshSessions.put(
                refreshToken,
                new RefreshSession(profile.getId(), Instant.now().plusMillis(jwtService.getRefreshExpiration())));
        return new AuthResponse(
                accessToken,
                accessToken,
                jwtService.getAccessExpiration() / 1000,
                refreshToken,
                jwtService.getRefreshExpiration() / 1000,
                profile.getId(),
                profile.getDisplayName());
    }

    private record RefreshSession(UUID userId, Instant expiresAt) {}
}
