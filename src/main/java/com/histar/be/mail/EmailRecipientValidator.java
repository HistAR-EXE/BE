package com.histar.be.mail;

final class EmailRecipientValidator {

    private EmailRecipientValidator() {}

    static String requireRecipient(String to) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Email recipient is required");
        }
        return to.trim();
    }
}
