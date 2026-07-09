package com.histar.be.mail;

import com.histar.be.config.HistarMailProperties;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;
    private final HistarMailProperties mailProperties;

    @Override
    public void sendHtml(String to, String subject, String html) {
        String recipient = EmailRecipientValidator.requireRecipient(to);
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new EmailDeliveryException("SMTP failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void sendText(String to, String subject, String text) {
        String recipient = EmailRecipientValidator.requireRecipient(to);
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(text, false);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new EmailDeliveryException("SMTP failed: " + ex.getMessage(), ex);
        }
    }
}
