package com.histar.be.mail;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.config.HistarMailProperties;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

class AttachmentProviderPayloadTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void bothProvidersTransmitBase64BytesWithFeatureSender() throws Exception {
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
            when(response.bodyToMono(Void.class)).thenReturn(Mono.empty());
            EmailSender sender = brevo ? new BrevoEmailSender(client, new HistarMailProperties())
                    : new ResendEmailSender(client, new HistarMailProperties());
            AttachmentEmailSenderTest.invoke(sender);
            var captured = ArgumentCaptor.forClass(Object.class);
            verify(body).bodyValue(captured.capture());
            var json = mapper.valueToTree(captured.getValue());
            var attachment = json.path(brevo ? "attachment" : "attachments").get(0);
            assertThat(attachment.path(brevo ? "name" : "filename").asText()).isEqualTo("passport.png");
            assertThat(Base64.getDecoder().decode(attachment.path("content").asText()))
                    .isEqualTo(AttachmentEmailSenderTest.PNG);
            if (brevo) {
                assertThat(json.path("sender").path("email").asText()).isEqualTo("passport@example.com");
            } else {
                assertThat(json.path("from").asText()).isEqualTo(AttachmentEmailSenderTest.FROM);
            }
        }
    }

    @Test
    void oldPayloadsDoNotAddNullAttachments() {
        var brevo = new BrevoEmailRequest(new BrevoEmailRequest.Sender(null, "a@example.com"),
                java.util.List.of(new BrevoEmailRequest.Recipient("b@example.com")), "subject", "html");
        var resend = new ResendEmailRequest("a@example.com", new String[]{"b@example.com"}, "subject", "html");
        assertThat(mapper.valueToTree(brevo).has("attachment")).isFalse();
        assertThat(mapper.valueToTree(resend).has("attachments")).isFalse();
    }
}
