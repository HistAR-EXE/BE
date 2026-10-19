package com.histar.be.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.histar.be.config.HistarMailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class AttachmentFailureTest {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void providerRejectionIsGenericAndDoesNotExposeProviderBody() {
        for (boolean brevo : new boolean[] {true, false}) {
            var client = mock(WebClient.class);
            var uri = mock(WebClient.RequestBodyUriSpec.class);
            var body = mock(WebClient.RequestBodySpec.class);
            var headers = mock(WebClient.RequestHeadersSpec.class);
            var response = mock(WebClient.ResponseSpec.class);
            when(client.post()).thenReturn(uri);
            when(uri.uri(brevo ? "/smtp/email" : "/emails")).thenReturn(body);
            when(body.bodyValue(any())).thenReturn(headers);
            when(headers.retrieve()).thenReturn(response);
            when(response.bodyToMono(Void.class)).thenReturn(Mono.error(new IllegalStateException("private@example.com SECRET")));
            EmailSender sender = brevo ? new BrevoEmailSender(client, new HistarMailProperties())
                    : new ResendEmailSender(client, new HistarMailProperties());
            assertThatThrownBy(() -> sender.sendAttachment(AttachmentEmailSenderTest.FROM, "user@example.com",
                    "Passport", "html", "passport.png", AttachmentEmailSenderTest.PNG))
                    .isInstanceOf(EmailDeliveryException.class)
                    .hasMessageNotContaining("SECRET").hasMessageNotContaining("private@example.com").hasNoCause();
        }
    }

    @Test
    void unsupportedLegacySenderCannotPretendAcceptance() {
        EmailSender legacy = new EmailSender() {
            public void sendHtml(String to, String subject, String html) {}
            public void sendText(String to, String subject, String text) {}
        };
        assertThatThrownBy(() -> AttachmentEmailSenderTest.invoke(legacy))
                .hasRootCauseInstanceOf(EmailDeliveryException.class);
    }
}
