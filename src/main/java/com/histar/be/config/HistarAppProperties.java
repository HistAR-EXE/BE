package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "histar.app")
@Getter
@Setter
public class HistarAppProperties {

    private String frontendUrl = "http://localhost:5173";
}
