package com.histar.be.auth.service.impl;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.auth.service.AuthService;
import com.histar.be.common.exception.ConflictException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.service.ProfileService;
import com.histar.be.security.JwtService;
import java.time.Instant;
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
        return new AuthResponse(
                jwtService.generateToken(profile.getEmail()), profile.getId(), profile.getDisplayName());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Profile profile = profileService
                .findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));
        return new AuthResponse(
                jwtService.generateToken(profile.getEmail()), profile.getId(), profile.getDisplayName());
    }
}
