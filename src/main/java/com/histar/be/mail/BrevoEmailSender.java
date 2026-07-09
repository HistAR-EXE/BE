package com.histar.be.mail;

import com.histar.be.config.HistarMailProperties;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class BrevoEmailSender implements EmailSender {

    private final WebClient brevoWebClient;
    private final HistarMailProperties mailProperties;

    public BrevoEmailSender(
            @Qualifier("brevoWebClient") WebClient brevoWebClient, HistarMailProperties mailProperties) {
        this.brevoWebClient = brevoWebClient;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        String recipient = EmailRecipientValidator.requireRecipient(to);
        BrevoEmailRequest request = new BrevoEmailRequest(
                parseSender(mailProperties.getFrom()),
                List.of(new BrevoEmailRequest.Recipient(recipient)),
                subject,
                html);
        dispatch(request);
    }

    @Override
    public void sendText(String to, String subject, String text) {
        sendHtml(to, subject, "<p>" + escapeHtml(text) + "</p>");
    }

    private BrevoEmailRequest.Sender parseSender(String fromConfig) {
        if (fromConfig == null || fromConfig.isBlank()) {
            throw new IllegalArgumentException("MAIL_FROM cannot be empty");
        }
        String from = stripSurroundingQuotes(fromConfig.trim());
        if (from.contains("<") && from.endsWith(">")) {
            int bracketIndex = from.indexOf("<");
            String name = from.substring(0, bracketIndex).trim();
            String email = from.substring(bracketIndex + 1, from.length() - 1).trim();
            if (email.isBlank()) {
                throw new IllegalArgumentException("MAIL_FROM email is required");
            }
            return new BrevoEmailRequest.Sender(name.isEmpty() ? null : name, email);
        }
        return new BrevoEmailRequest.Sender(null, from);
    }

    private void dispatch(BrevoEmailRequest request) {
        try {
            brevoWebClient
                    .post()
                    .uri("/smtp/email")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Brevo API error status={} body={}", ex.getStatusCode(), body);
            throw new EmailDeliveryException("Brevo: " + body, ex);
        } catch (Exception ex) {
            log.warn("Brevo API error: {}", ex.getMessage(), ex);
            throw new EmailDeliveryException("Brevo: " + ex.getMessage(), ex);
        }
    }

    private static String stripSurroundingQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
