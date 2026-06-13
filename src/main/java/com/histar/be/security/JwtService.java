package com.histar.be.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return generateToken(email, null, null, expiration);
    }

    public String generateAccessToken(UUID userId, String email, String role) {
        return generateToken(email, userId, role, expiration);
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .subject(email)
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(key())
                .compact();
    }

    public long getAccessExpiration() {
        return expiration;
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }

    private String generateToken(String email, UUID userId, String role, long ttlMillis) {
        var builder = Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis));
        if (userId != null) {
            builder.claim("userId", userId.toString());
        }
        if (role != null && !role.isBlank()) {
            builder.claim("role", role);
        }
        return builder.signWith(key()).compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Optional<UUID> extractUserId(String token) {
        Object raw = parseClaims(token).get("userId");
        if (raw == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw.toString()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public Optional<String> extractRole(String token) {
        Object raw = parseClaims(token).get("role");
        if (raw == null) {
            return Optional.empty();
        }
        String role = raw.toString().trim();
        return role.isEmpty() ? Optional.empty() : Optional.of(role);
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
