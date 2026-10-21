package com.histar.be.mail;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.config.HistarMailProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ArCardHtmlProviderTest {
    @Test @SuppressWarnings({"rawtypes","unchecked"})
    void bothHttpProvidersUseBoundedHtmlDeliveryAndPropagateFailure() {
        for(boolean brevo : new boolean[]{true,false}) {
            var client=mock(WebClient.class);var uri=mock(WebClient.RequestBodyUriSpec.class);
            var body=mock(WebClient.RequestBodySpec.class);var headers=mock(WebClient.RequestHeadersSpec.class);
            var response=mock(WebClient.ResponseSpec.class);Mono<Void> mono=mock(Mono.class);
            when(client.post()).thenReturn(uri);when(uri.uri(brevo?"/smtp/email":"/emails")).thenReturn(body);
            when(body.bodyValue(any())).thenReturn(headers);when(headers.retrieve()).thenReturn(response);
            when(response.bodyToMono(Void.class)).thenReturn(mono);
            EmailSender sender=brevo?new BrevoEmailSender(client,new HistarMailProperties()):new ResendEmailSender(client,new HistarMailProperties());
            sender.sendHtmlBounded("owner@example.invalid","AR cards","<p>10 color cards</p>");
            verify(mono).block(Duration.ofSeconds(20));
            when(mono.block(Duration.ofSeconds(20))).thenThrow(new IllegalStateException("timeout"));
            assertThrows(EmailDeliveryException.class,()->sender.sendHtmlBounded("owner@example.invalid","AR cards","<p>10 color cards</p>"));
        }
    }
}
