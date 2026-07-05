package com.histar.be.auth.service.impl;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.LogoutRequest;
import com.histar.be.auth.dto.RefreshTokenRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.auth.entity.RefreshToken;
import com.histar.be.auth.repository.RefreshTokenRepository;
import com.histar.be.auth.service.AuthService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.organization.entity.OrgSubscription;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.profile.service.ProfileService;
import com.histar.be.security.JwtService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ProfileService profileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (profileService.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("Email đã được đăng ký");
        }
        Profile profile = Profile.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .provider("local")
                .role(UserRole.USER.name())
                .tier(UserTier.FREE.name())
                .orgSubscription(OrgSubscription.NONE.name())
                .level(1)
                .totalPoints(0)
                .createdAt(Instant.now())
                .build();
        profileService.save(profile);
        return issueTokens(profile);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Profile profile = profileService
                .findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
        return issueTokens(profile);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.refreshToken())
                .orElseThrow(() -> new AuthException("Refresh token không hợp lệ hoặc đã hết hạn"));
        if (stored.getExpiresAt().isBefore(Instant.now()) || !jwtService.isValid(request.refreshToken())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new AuthException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
        Profile profile = profileService.findById(stored.getUserId());
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return issueTokens(profile);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenAndRevokedFalse(request.refreshToken()).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueTokens(Profile profile) {
        String role = UserRole.fromStored(profile.getRole()).name();
        String orgSub = OrgSubscription.fromStored(profile.getOrgSubscription()).name();
        String accessToken = jwtService.generateAccessToken(
                profile.getId(), profile.getEmail(), role, profile.getOrgId(), orgSub);
        String refreshToken = jwtService.generateRefreshToken(profile.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(profile.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshExpiration()))
                .revoked(false)
                .createdAt(Instant.now())
                .build());
        return new AuthResponse(
                accessToken,
                accessToken,
                jwtService.getAccessExpiration() / 1000,
                refreshToken,
                jwtService.getRefreshExpiration() / 1000,
                profile.getId(),
                profile.getDisplayName(),
                role,
                UserTier.fromStored(profile.getTier()).name(),
                profile.getOrgId(),
                orgSub);
    }
}
