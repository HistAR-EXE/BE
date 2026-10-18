package com.histar.be.mail;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record BrevoEmailRequest(
        Sender sender, List<Recipient> to, String subject, String htmlContent,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Attachment> attachment) {
    public BrevoEmailRequest(Sender sender, List<Recipient> to, String subject, String htmlContent) {
        this(sender, to, subject, htmlContent, null);
    }
    public record Attachment(String name, String content) {}
    public record Sender(String name, String email) {}
    public record Recipient(String email, String name) {
        public Recipient(String email) { this(email, null); }
    }
}
