package com.histar.be.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.TestHookProperties;
import jakarta.mail.Session;
import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class AttachmentEmailSenderTest {
    static final String FROM = "TimeLens <passport@example.com>";
    static final byte[] PNG = { (byte)137, 80, 78, 71, 1, 2, 3 };

    @Test
    void attachmentContractExists() {
        assertThatCode(() -> EmailSender.class.getMethod("sendAttachment", String.class, String.class,
                String.class, String.class, String.class, byte[].class)).doesNotThrowAnyException();
    }

    @Test
    void smtpIncludesExactPngAndSeparateSender() throws Exception {
        var transport = mock(JavaMailSender.class);
        var message = new MimeMessage(Session.getInstance(new Properties()));
        when(transport.createMimeMessage()).thenReturn(message);
        var config = new HistarMailProperties();
        config.setFrom("otp@example.com");
        invoke(new SmtpEmailSender(transport, config));
        message.saveChanges();
        assertThat(message.getFrom()[0].toString()).isEqualTo(FROM);
        Multipart mixed = (Multipart) message.getContent();
        var attachment = mixed.getBodyPart(mixed.getCount() - 1);
        assertThat(attachment.getFileName()).isEqualTo("passport.png");
        assertThat(attachment.isMimeType("image/png")).isTrue();
        assertThat(attachment.getInputStream().readAllBytes()).isEqualTo(PNG);
        assertThat(config.getFrom()).isEqualTo("otp@example.com");
        verify(transport).send(message);
    }

    @Test
    void disabledAndTestHooksCannotPretendAcceptance() {
        var config = new HistarMailProperties();
        var hooks = new TestHookProperties();
        var sender = mock(EmailSender.class);
        var service = new HistarEmailService(config, hooks, sender);
        config.setEnabled(false);
        assertThatThrownBy(() -> invoke(service)).hasRootCauseInstanceOf(EmailDeliveryException.class);
        config.setEnabled(true);
        hooks.setEnabled(true);
        assertThatThrownBy(() -> invoke(service)).hasRootCauseInstanceOf(EmailDeliveryException.class);
        verifyNoInteractions(sender);
    }

    @Test
    void smtpRejectsHeaderInjectionBeforeDelivery() {
        var transport = mock(JavaMailSender.class);
        var sender = new SmtpEmailSender(transport, new HistarMailProperties());
        assertThatThrownBy(() -> sender.getClass().getMethod("sendAttachment", String.class, String.class,
                String.class, String.class, String.class, byte[].class).invoke(sender,
                FROM, "user@example.com\r\nBcc: other@example.com", "Passport", "html", "passport.png", PNG))
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(transport);
    }

    static void invoke(Object target) throws Exception {
        target.getClass().getMethod("sendAttachment", String.class, String.class, String.class,
                String.class, String.class, byte[].class)
                .invoke(target, FROM, "user@example.com", "Passport", "<p>Passport</p>", "passport.png", PNG);
    }
}
