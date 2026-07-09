package com.histar.be.mail;

import com.histar.be.config.HistarMailProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@Slf4j
public class ResendEmailSender implements EmailSender {

    private final WebClient resendWebClient;
    private final HistarMailProperties mailProperties;

    public ResendEmailSender(
            @Qualifier("resendWebClient") WebClient resendWebClient, HistarMailProperties mailProperties) {
        this.resendWebClient = resendWebClient;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendHtml(String to, String subject, String html) {
        String recipient = EmailRecipientValidator.requireRecipient(to);
        ResendEmailRequest request = new ResendEmailRequest(
                mailProperties.getFrom(), new String[] {recipient}, subject, html);
        dispatch(request);
    }

    @Override
    public void sendText(String to, String subject, String text) {
        String recipient = EmailRecipientValidator.requireRecipient(to);
        String html = "<p>" + escapeHtml(text) + "</p>";
        ResendEmailRequest request = new ResendEmailRequest(
                mailProperties.getFrom(), new String[] {recipient}, subject, html);
        dispatch(request);
    }

    private void dispatch(ResendEmailRequest request) {
        try {
            resendWebClient
                    .post()
                    .uri("/emails")
                    .bodyValue(request)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            String body = ex.getResponseBodyAsString();
            log.warn("Resend API error status={} body={}", ex.getStatusCode(), body);
            throw new EmailDeliveryException("Resend: " + body, ex);
        } catch (Exception ex) {
            log.warn("Resend API error: {}", ex.getMessage(), ex);
            throw new EmailDeliveryException("Resend: " + ex.getMessage(), ex);
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
