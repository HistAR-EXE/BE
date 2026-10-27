package com.histar.be.profile.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.net.URI;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80) String displayName,
        @Size(max = 512) String avatarUrl,
        @Size(max = 120) String city) {
    public UpdateProfileRequest {
        displayName = trimUnicode(displayName);
        avatarUrl = trimUnicode(avatarUrl);
        city = trimUnicode(city);
    }

    private static String trimUnicode(String value) {
        return value == null ? null : value.replaceAll("(?U)^\\s+|\\s+$", "");
    }

    @AssertTrue(message = "Ảnh đại diện phải là URL http(s) hoặc đường dẫn nội bộ hợp lệ")
    public boolean isAvatarUrlValid() {
        if (avatarUrl == null || avatarUrl.isEmpty()) return true;
        if (avatarUrl.indexOf('\\') >= 0 || avatarUrl.chars().anyMatch(Character::isISOControl)) return false;
        if (avatarUrl.startsWith("/") && !avatarUrl.startsWith("//")) {
            try { return new URI(avatarUrl).getRawAuthority() == null; }
            catch (java.net.URISyntaxException invalid) { return false; }
        }
        try {
            URI uri = new URI(avatarUrl);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null;
        } catch (java.net.URISyntaxException invalid) { return false; }
    }
}
