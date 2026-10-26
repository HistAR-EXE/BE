package com.histar.be.admin.service;

import com.histar.be.admin.dto.DemoAdminSessionResponse;
import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.security.JwtService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoAdminSessionService {

    private static final long TICKET_TTL_SECONDS = 120;
    private static final long SESSION_TTL_MILLIS = 15 * 60 * 1000L;
    private final DemoProperties properties;
    private final ProfileRepository profiles;
    private final JwtService jwtService;
    private final Map<String, Long> consumedTickets = new ConcurrentHashMap<>();

    public DemoAdminSessionResponse issue(String email) {
        if (properties.isEnabled()) {
            throw new BusinessRuleException("Demo admin tickets must be issued by the production environment");
        }
        requireBridgeConfiguration();
        Instant expiresAt = Instant.now().plusSeconds(TICKET_TTL_SECONDS);
        String payload = email.toLowerCase() + "\n" + expiresAt.getEpochSecond() + "\n" + UUID.randomUUID();
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String ticket = encoded + "." + sign(encoded);
        String url = properties.getFrontendUrl().replaceAll("/+$", "")
                + "/demo/admin-session?ticket=" + URLEncoder.encode(ticket, StandardCharsets.UTF_8);
        return new DemoAdminSessionResponse(url, expiresAt);
    }

    public AuthResponse exchange(String ticket) {
        if (!properties.isEnabled()) {
            throw new BusinessRuleException("Demo admin exchange is only available in the demo environment");
        }
        requireBridgeConfiguration();
        String[] parts = ticket.split("\\.", 2);
        if (parts.length != 2 || !MessageDigest.isEqual(
                sign(parts[0]).getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessRuleException("Demo admin ticket is invalid");
        }
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Demo admin ticket is invalid");
        }
        String[] values = payload.split("\n", 3);
        if (values.length != 3) throw new BusinessRuleException("Demo admin ticket is invalid");
        long expiresAt;
        try {
            expiresAt = Long.parseLong(values[1]);
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Demo admin ticket is invalid");
        }
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new BusinessRuleException("Demo admin ticket has expired");
        }
        String email = values[0].toLowerCase();
        if (!email.equalsIgnoreCase(properties.getAdminEmail())) {
            throw new BusinessRuleException("Demo admin ticket is not for the configured administrator");
        }
        long now = Instant.now().getEpochSecond();
        consumedTickets.entrySet().removeIf(entry -> entry.getValue() < now);
        if (consumedTickets.putIfAbsent(parts[1], expiresAt) != null) {
            throw new BusinessRuleException("Demo admin ticket has already been used");
        }
        Profile profile = profiles.findByEmail(email)
                .filter(value -> "ADMIN".equalsIgnoreCase(value.getRole()))
                .filter(value -> "ACTIVE".equalsIgnoreCase(value.getAccountStatus()))
                .orElseThrow(() -> new BusinessRuleException("Demo administrator is unavailable"));
        String access = jwtService.generateAccessTokenForDuration(
                profile.getId(), profile.getEmail(), profile.getRole(), profile.getOrgId(), profile.getOrgSubscription(), SESSION_TTL_MILLIS);
        return new AuthResponse(access, access, SESSION_TTL_MILLIS / 1000, null, null,
                profile.getId(), profile.getDisplayName(), profile.getEmail(), profile.getRole(), profile.getTier(),
                profile.getOrgId(), profile.getOrgSubscription(), Boolean.TRUE.equals(profile.getEmailVerified()), null);
    }

    private void requireBridgeConfiguration() {
        if (properties.getAdminBridgeSecret() == null || properties.getAdminBridgeSecret().length() < 32
                || properties.getFrontendUrl() == null || properties.getFrontendUrl().isBlank()
                || properties.getAdminEmail() == null || properties.getAdminEmail().isBlank()) {
            throw new BusinessRuleException("Demo admin bridge is not configured");
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getAdminBridgeSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign demo admin ticket", ex);
        }
    }
}
