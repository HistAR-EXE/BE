package com.histar.be.mail;

import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HistarEmailService {

    private final HistarMailProperties mailProperties;
    private final TestHookProperties testHookProperties;
    private final EmailSender emailSender;

    public void sendHtml(String to, String subject, String html) {
        if (!mailProperties.isEnabled()) {
            log.info("Email sending is disabled. Skipping HTML mail to {} subject={}", to, subject);
            return;
        }
        if (testHookProperties.isEnabled()) {
            log.info("Test hooks enabled — skip mail delivery to {} subject={}", to, subject);
            return;
        }
        String provider = resolveProviderLabel();
        log.info("Sending email via {} to {} subject={}", provider, to, subject);
        try {
            emailSender.sendHtml(to, subject, html);
        } catch (EmailDeliveryException ex) {
            log.warn("Failed to send HTML email via {} to {}: {}", provider, to, ex.getMessage());
            throw ex;
        }
    }

    public void sendText(String to, String subject, String text) {
        if (!mailProperties.isEnabled()) {
            log.info("Email sending is disabled. Skipping text mail to {} subject={}", to, subject);
            return;
        }
        if (testHookProperties.isEnabled()) {
            log.info("Test hooks enabled — skip mail delivery to {} subject={}", to, subject);
            return;
        }
        String provider = resolveProviderLabel();
        log.info("Sending email via {} to {} subject={}", provider, to, subject);
        try {
            emailSender.sendText(to, subject, text);
        } catch (EmailDeliveryException ex) {
            log.warn("Failed to send text email via {} to {}: {}", provider, to, ex.getMessage());
            throw ex;
        }
    }

    private String resolveProviderLabel() {
        String provider = mailProperties.getProvider();
        return provider != null && !provider.isBlank() ? provider.trim().toLowerCase() : "smtp";
    }
}
