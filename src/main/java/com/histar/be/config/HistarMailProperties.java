package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "histar.mail")
@Getter
@Setter
public class HistarMailProperties {

    private boolean enabled = true;
    private String from = "noreply@histar.vn";
    private int verificationTtlHours = 24;
    private int resendCooldownSeconds = 60;
}
