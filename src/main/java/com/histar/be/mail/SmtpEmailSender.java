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
    public void sendHtmlBounded(String to, String subject, String html) {
        var recipient=EmailRecipientValidator.requireRecipient(to);
        try {
            var bounded=attachmentSender();
            var message=bounded.createMimeMessage();
            var helper=new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());helper.setTo(recipient);
            helper.setSubject(subject);helper.setText(html,true);bounded.send(message);
        } catch(Exception error) { throw new EmailDeliveryException("SMTP HTML delivery failed"); }
    }

    @Override
    public void sendAttachment(String from, String to, String subject, String html, String filename, byte[] png) {
        AttachmentMailValidator.validate(from, to, subject, filename, png);
        try {
            JavaMailSender boundedSender = attachmentSender();
            var message = boundedSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(png), "image/png");
            boundedSender.send(message);
        } catch (Exception ex) {
            throw new EmailDeliveryException("SMTP attachment delivery failed");
        }
    }

    private JavaMailSender attachmentSender() {
        if (!(mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl original)) {
            return mailSender;
        }
        // Keep OTP sender/session untouched; bound only this per-message transport.
        var bounded = new org.springframework.mail.javamail.JavaMailSenderImpl();
        bounded.setHost(original.getHost());
        bounded.setPort(original.getPort());
        bounded.setProtocol(original.getProtocol());
        bounded.setUsername(original.getUsername());
        bounded.setPassword(original.getPassword());
        bounded.setDefaultEncoding(original.getDefaultEncoding());
        var properties = new java.util.Properties();
        properties.putAll(original.getSession().getProperties());
        for (String protocol : new String[] {"smtp", "smtps"}) {
            for (String timeout : new String[] {"connectiontimeout", "timeout", "writetimeout"}) {
                properties.setProperty("mail." + protocol + "." + timeout, "20000");
            }
        }
        bounded.setJavaMailProperties(properties);
        return bounded;
    }

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
