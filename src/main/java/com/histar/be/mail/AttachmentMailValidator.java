package com.histar.be.mail;

import jakarta.mail.internet.InternetAddress;

/** Validation for the controlled single-recipient PNG attachment path. */
final class AttachmentMailValidator {
    private AttachmentMailValidator() {}

    static void validate(String from, String to, String subject, String filename, byte[] png) {
        header(from);
        header(to);
        header(subject);
        address(from);
        address(to);
        if (filename == null || !filename.matches("[A-Za-z0-9_-]+\\.png")) {
            throw new IllegalArgumentException("Invalid PNG attachment filename");
        }
        if (png == null || png.length == 0) {
            throw new IllegalArgumentException("PNG attachment is required");
        }
    }

    private static void header(String value) {
        if (value == null || value.isBlank() || value.chars().anyMatch(c -> c < 32 || c == 127)) {
            throw new IllegalArgumentException("Invalid attachment mail header");
        }
    }

    private static void address(String value) {
        try {
            InternetAddress[] addresses = InternetAddress.parse(value, true);
            if (addresses.length != 1 || addresses[0].isGroup()) {
                throw new IllegalArgumentException("One mail address is required");
            }
            addresses[0].validate();
        } catch (jakarta.mail.internet.AddressException ex) {
            throw new IllegalArgumentException("Invalid mail address");
        }
    }
}
