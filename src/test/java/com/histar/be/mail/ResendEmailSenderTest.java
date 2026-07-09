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
class ResendEmailSenderTest {

    @Mock
    private WebClient resendWebClient;

    @Mock
    private HistarMailProperties mailProperties;

    private ResendEmailSender resendEmailSender;

    @BeforeEach
    void setUp() {
        resendEmailSender = new ResendEmailSender(resendWebClient, mailProperties);
    }

    @Test
    void sendHtml_blankRecipient_throwsBeforeApiCall() {
        assertThatThrownBy(() -> resendEmailSender.sendHtml("  ", "subject", "<p>hi</p>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
        verify(resendWebClient, never()).post();
    }

    @Test
    void sendHtml_nullRecipient_throwsBeforeApiCall() {
        assertThatThrownBy(() -> resendEmailSender.sendHtml(null, "subject", "<p>hi</p>"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(resendWebClient, never()).post();
    }

    @Test
    void sendHtml_resendApi403_includesResponseBody() {
        when(mailProperties.getFrom()).thenReturn("TimeLens <onboarding@resend.dev>");
        stubResendApiError(403, "{\"message\":\"domain not verified\"}");

        assertThatThrownBy(() -> resendEmailSender.sendHtml("user@example.com", "subject", "<p>hi</p>"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("domain not verified");
    }

    @SuppressWarnings("unchecked")
    private void stubResendApiError(int status, String body) {
        RequestBodyUriSpec requestBodyUriSpec = mock(RequestBodyUriSpec.class);
        RequestBodySpec requestBodySpec = mock(RequestBodySpec.class);
        RequestHeadersSpec requestHeadersSpec = mock(RequestHeadersSpec.class);
        ResponseSpec responseSpec = mock(ResponseSpec.class);
        when(resendWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/emails")).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity())
                .thenReturn(Mono.error(WebClientResponseException.create(
                        status, "Error", null, body.getBytes(), null)));
    }
}
