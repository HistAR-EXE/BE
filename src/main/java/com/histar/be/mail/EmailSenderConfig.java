package com.histar.be.mail;

import com.histar.be.config.HistarMailProperties;
import com.histar.be.config.ResendProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@Slf4j
public class EmailSenderConfig {

    @Bean
    @Qualifier("resendWebClient")
    WebClient resendWebClient(ResendProperties props) {
        return WebClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    EmailSender emailSender(
            HistarMailProperties mailProperties,
            SmtpEmailSender smtpEmailSender,
            ResendEmailSender resendEmailSender) {
        String provider = mailProperties.getProvider() != null
                ? mailProperties.getProvider().trim().toLowerCase()
                : "smtp";
        if ("resend".equals(provider)) {
            log.info("Mail provider: resend (HTTPS API)");
            return resendEmailSender;
        }
        log.info("Mail provider: smtp");
        return smtpEmailSender;
    }
}
