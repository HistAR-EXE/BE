package com.histar.be.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "histar.mail")
@Getter
@Setter
public class HistarMailProperties {

    private boolean enabled = true;
    /** smtp | resend | brevo */
    private String provider = "smtp";
    private String from = "noreply@histar.vn";
    private int verificationTtlHours = 24;
    private int resendCooldownSeconds = 60;
    /** Password-reset OTP lifetime (minutes). */
    private int passwordResetOtpTtlMinutes = 10;
    /** Max wrong OTP attempts per challenge. */
    private int passwordResetMaxAttempts = 5;
    /** Reset token lifetime after OTP verified (minutes). */
    private int passwordResetTokenTtlMinutes = 15;
}
