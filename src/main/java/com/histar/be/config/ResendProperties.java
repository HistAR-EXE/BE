package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resend")
@Getter
@Setter
public class ResendProperties {

    private String apiKey = "";
}
