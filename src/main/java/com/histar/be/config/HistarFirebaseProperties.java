package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "histar.firebase")
@Getter
@Setter
public class HistarFirebaseProperties {

    private boolean enabled = false;

    /** Raw JSON or base64-encoded service account JSON. */
    private String serviceAccountJson = "";
}
