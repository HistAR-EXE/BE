package com.histar.be.mail;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.config.HistarMailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestBodyUriSpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class BrevoEmailSenderTest {

    @Mock
    private WebClient brevoWebClient;

    @Mock
    private HistarMailProperties mailProperties;

    private BrevoEmailSender brevoEmailSender;

    @BeforeEach
    void setUp() {
        brevoEmailSender = new BrevoEmailSender(brevoWebClient, mailProperties);
    }

    @Test
    void sendHtml_blankRecipient_throwsBeforeApiCall() {
        assertThatThrownBy(() -> brevoEmailSender.sendHtml("  ", "subject", "<p>hi</p>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
        verify(brevoWebClient, never()).post();
    }

    @Test
    void sendHtml_fromWithName_parsesSender() {
        when(mailProperties.getFrom()).thenReturn("TimeLens <noreply@example.com>");
        stubBrevoSuccess();

        brevoEmailSender.sendHtml("user@example.com", "subject", "<p>hi</p>");

        verify(requestBodySpec).bodyValue(new BrevoEmailRequest(
                new BrevoEmailRequest.Sender("TimeLens", "noreply@example.com"),
                java.util.List.of(new BrevoEmailRequest.Recipient("user@example.com")),
                "subject",
                "<p>hi</p>"));
    }

    @Test
    void sendHtml_fromEmailOnly_parsesSenderWithoutName() {
        when(mailProperties.getFrom()).thenReturn("noreply@example.com");
        stubBrevoSuccess();

        brevoEmailSender.sendHtml("user@example.com", "subject", "<p>hi</p>");

        verify(requestBodySpec).bodyValue(new BrevoEmailRequest(
                new BrevoEmailRequest.Sender(null, "noreply@example.com"),
                java.util.List.of(new BrevoEmailRequest.Recipient("user@example.com")),
                "subject",
                "<p>hi</p>"));
    }

    @Test
    void sendHtml_missingFrom_throwsClearly() {
        when(mailProperties.getFrom()).thenReturn(" ");

        assertThatThrownBy(() -> brevoEmailSender.sendHtml("user@example.com", "subject", "<p>hi</p>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAIL_FROM");
        verify(brevoWebClient, never()).post();
    }

    @Test
    void sendHtml_brevoApiError_includesResponseBody() {
        when(mailProperties.getFrom()).thenReturn("TimeLens <noreply@example.com>");
        stubBrevoError(400, "{\"message\":\"sender not verified\"}");

        assertThatThrownBy(() -> brevoEmailSender.sendHtml("user@example.com", "subject", "<p>hi</p>"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("sender not verified");
    }

    private RequestBodySpec requestBodySpec;

    @SuppressWarnings("unchecked")
    private void stubBrevoSuccess() {
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        requestBodySpec = mock(RequestBodySpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        when(brevoWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/smtp/email")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    @SuppressWarnings("unchecked")
    private void stubBrevoError(int status, String body) {
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        requestBodySpec = mock(RequestBodySpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        when(brevoWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/smtp/email")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class))
                .thenReturn(Mono.error(WebClientResponseException.create(
                        status, "Error", null, body.getBytes(), null)));
    }
}
