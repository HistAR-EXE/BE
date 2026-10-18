package com.histar.be.mail;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

public record ResendEmailRequest(String from, String[] to, String subject, String html,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<Attachment> attachments) {
    public ResendEmailRequest(String from, String[] to, String subject, String html) {
        this(from, to, subject, html, null);
    }
    public record Attachment(String filename, String content) {}
}
