package com.histar.be.mail;

import static org.assertj.core.api.Assertions.assertThat;

import com.histar.be.config.BrevoProperties;
import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.ResendProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class EmailSenderConfigTest {

    private final EmailSenderConfig config = new EmailSenderConfig();

    @Test
    void emailSender_providerBrevo_returnsBrevoSender() {
        HistarMailProperties mailProperties = new HistarMailProperties();
        mailProperties.setProvider("brevo");
        SmtpEmailSender smtpEmailSender = org.mockito.Mockito.mock(SmtpEmailSender.class);
        ResendEmailSender resendEmailSender = org.mockito.Mockito.mock(ResendEmailSender.class);
        BrevoEmailSender brevoEmailSender = org.mockito.Mockito.mock(BrevoEmailSender.class);

        EmailSender selected = config.emailSender(mailProperties, smtpEmailSender, resendEmailSender, brevoEmailSender);

        assertThat(selected).isSameAs(brevoEmailSender);
    }

    @Test
    void brevoWebClient_usesBrevoApiBaseUrl() {
        BrevoProperties properties = new BrevoProperties();
        properties.setApiKey("brevo-key");

        WebClient webClient = config.brevoWebClient(properties);

        assertThat(webClient).isNotNull();
    }
}
