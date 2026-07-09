package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "brevo")
@Getter
@Setter
public class BrevoProperties {

    private String apiKey;
}
