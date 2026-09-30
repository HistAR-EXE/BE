package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * QR-first presence check-in settings.
 *
 * <p>When {@code qrHmacSecret} is blank the station QR is accepted unsigned (DEMO mode only).
 * When set, every station QR must carry a valid HMAC-SHA256 signature and be within
 * {@code qrMaxSkewMinutes} of server time.
 */
@ConfigurationProperties(prefix = "presence")
public class PresenceProperties {

    private String qrHmacSecret = "";
    private int qrMaxSkewMinutes = 15;

    public String getQrHmacSecret() {
        return qrHmacSecret;
    }

    public void setQrHmacSecret(String qrHmacSecret) {
        this.qrHmacSecret = qrHmacSecret == null ? "" : qrHmacSecret;
    }

    public int getQrMaxSkewMinutes() {
        return qrMaxSkewMinutes;
    }

    public void setQrMaxSkewMinutes(int qrMaxSkewMinutes) {
        this.qrMaxSkewMinutes = qrMaxSkewMinutes;
    }

    public boolean isSigningEnabled() {
        return qrHmacSecret != null && !qrHmacSecret.isBlank();
    }
}
