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
    /** SePay dashboard API key. Real webhooks send `Authorization: Apikey <key>`. */
    private String apiKey = "";
    /** auto = Apikey header if present, otherwise HMAC. apikey | hmac force one scheme. */
    private String webhookAuthMode = "auto";
    private String qrBaseUrl = "https://vietqr.app/img";
    private String qrTemplate = "compact";
    private boolean qrShowInfo = true;
    private int b2cPremiumPriceVnd = 79_000;
    private int orderExpiryMinutes = 15;
    /** Keep polling PENDING this long after expiresAt so a late webhook can still upgrade. */
    private int expiryGraceMinutes = 30;
    private long maxTimestampSkewSeconds = 300;
}
