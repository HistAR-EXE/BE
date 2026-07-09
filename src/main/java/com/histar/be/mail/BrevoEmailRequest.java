package com.histar.be.mail;

import java.util.List;

public record BrevoEmailRequest(
        Sender sender,
        List<Recipient> to,
        String subject,
        String htmlContent) {

    public record Sender(String name, String email) {}

    public record Recipient(String email, String name) {
        public Recipient(String email) {
            this(email, null);
        }
    }
}
