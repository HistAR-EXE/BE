package com.histar.be.profile.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.net.URI;

public record UpdateProfileRequest(
        @Size(min = 2, max = 80) String displayName,
        @Size(max = 512) String avatarUrl,
        @Size(max = 120) String city,
        @Size(max = 120) String fullName,
        @Size(max = 10) String dateOfBirth,
        @jakarta.validation.constraints.Pattern(regexp = "^(|MALE|FEMALE|OTHER|UNDISCLOSED)$") String gender,
        @jakarta.validation.constraints.Pattern(regexp = "^$|^[+0-9][0-9 ()-]{6,24}$") String phone,
        @Size(max = 1000) String bio,
        @Size(max = 200) String school,
        @Size(max = 80) String className,
        @Size(max = 200) String specialty) {
    public UpdateProfileRequest(String displayName, String avatarUrl, String city) {
        this(displayName, avatarUrl, city, null, null, null, null, null, null, null, null);
    }
    public UpdateProfileRequest {
        displayName = trimUnicode(displayName);
        avatarUrl = trimUnicode(avatarUrl);
        city = trimUnicode(city);
        fullName = trimUnicode(fullName);
        gender = trimUnicode(gender);
        phone = trimUnicode(phone);
        bio = trimUnicode(bio);
        school = trimUnicode(school);
        className = trimUnicode(className);
        specialty = trimUnicode(specialty);

    }

    @AssertTrue(message = "Ngày sinh không hợp lệ hoặc nằm trong tương lai")
    public boolean isDateOfBirthValid() {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) return true;
        try { return !java.time.LocalDate.parse(dateOfBirth).isAfter(java.time.LocalDate.now()); }
        catch (java.time.format.DateTimeParseException ex) { return false; }
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
