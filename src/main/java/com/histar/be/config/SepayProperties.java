package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.sepay")
@Getter
@Setter
public class SepayProperties {

    private boolean enabled = false;
    private String bankCode = "";
    private String accountNumber = "";
    private String accountName = "";
    private String webhookSecret = "";
    private String qrBaseUrl = "https://vietqr.app/img";
    private String qrTemplate = "compact";
    private boolean qrShowInfo = true;
    private int b2cPremiumPriceVnd = 49_000;
    private int orderExpiryMinutes = 15;
    private long maxTimestampSkewSeconds = 300;
}
