package com.histar.be.checkin.presence;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.PresenceProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Station QR payloads:
 * <ul>
 *   <li>Static (print): {@code siteCode:stationCode:0:sig} —
 *       {@code sig = hex(HMAC-SHA256(secret, "siteCode:stationCode:0"))}</li>
 *   <li>Legacy dynamic: {@code stationCode:timestamp:sig} (assumes site {@code cu-chi})</li>
 *   <li>Site dynamic: {@code siteCode:stationCode:timestamp:sig}</li>
 * </ul>
 * Timestamp {@code 0} never expires (field-printed QR). Non-zero timestamps enforce
 * {@code presence.qr-max-skew-minutes}.
 */
@Service
@Slf4j
public class StationQrService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern STATION_CODE = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final Pattern SITE_CODE = Pattern.compile("[a-z0-9][a-z0-9-]{0,63}");
    public static final String DEFAULT_SITE = "cu-chi";
    public static final long STATIC_TIMESTAMP = 0L;

    private final PresenceProperties properties;
    private final Clock clock;

    @Autowired
    public StationQrService(PresenceProperties properties) {
        this(properties, Clock.systemUTC());
    }

    StationQrService(PresenceProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public record SignedStationQr(
            String siteCode,
            String stationCode,
            long timestamp,
            String signature,
            String payload,
            boolean signed,
            boolean staticQr) {}

    public record VerifiedStationQr(String siteCode, String stationCode) {}

    public static String normalizeStationCode(String stationCode) {
        if (stationCode == null || !STATION_CODE.matcher(stationCode.trim()).matches()) {
            throw new BusinessRuleException("stationCode không hợp lệ");
        }
        return stationCode.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeSiteCode(String siteCode) {
        if (siteCode == null || siteCode.isBlank()) {
            return DEFAULT_SITE;
        }
        String s = siteCode.trim().toLowerCase(Locale.ROOT);
        if (!SITE_CODE.matcher(s).matches()) {
            throw new BusinessRuleException("siteCode không hợp lệ");
        }
        return s;
    }

    /** Issues a static printable QR (timestamp 0) for waterproof station signs. */
    public SignedStationQr issueStatic(String siteCode, String stationCode) {
        return issue(siteCode, stationCode, STATIC_TIMESTAMP);
    }

    /** Issues a short-lived dynamic QR (current epoch seconds). */
    public SignedStationQr issue(String stationCode) {
        return issue(DEFAULT_SITE, stationCode, clock.instant().getEpochSecond());
    }

    public SignedStationQr issue(String siteCode, String stationCode) {
        return issue(siteCode, stationCode, clock.instant().getEpochSecond());
    }

    public SignedStationQr issue(String siteCode, String stationCode, long timestamp) {
        String site = normalizeSiteCode(siteCode);
        String code = normalizeStationCode(stationCode);
        boolean signed = properties.isSigningEnabled();
        String signature = signed ? sign(site, code, timestamp) : "";
        String payload = site + ":" + code + ":" + timestamp + ":" + signature;
        return new SignedStationQr(site, code, timestamp, signature, payload, signed, timestamp == STATIC_TIMESTAMP);
    }

    /** Verifies payload and returns site + station. */
    public VerifiedStationQr verifyDetailed(String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            throw new BusinessRuleException("Mã QR trạm không hợp lệ");
        }
        String[] parts = qrPayload.trim().split(":", -1);
        String site;
        String code;
        long ts;
        String sig;

        if (parts.length == 4) {
            site = normalizeSiteCode(parts[0]);
            code = normalizeStationCode(parts[1]);
            ts = parseTimestamp(parts[2]);
            sig = parts[3];
        } else if (parts.length == 3 || parts.length == 2) {
            // Legacy: stationCode:timestamp[:sig] → cu-chi
            site = DEFAULT_SITE;
            code = normalizeStationCode(parts[0]);
            ts = parseTimestamp(parts[1]);
            sig = parts.length == 3 ? parts[2] : "";
        } else {
            throw new BusinessRuleException("Mã QR trạm không hợp lệ");
        }

        if (!properties.isSigningEnabled()) {
            log.warn(
                    "presence.qr-hmac-secret is blank: accepting UNSIGNED station QR for {}/{} (DEMO only)",
                    site,
                    code);
            return new VerifiedStationQr(site, code);
        }

        if (sig == null || sig.isBlank()) {
            throw new BusinessRuleException("Mã QR trạm thiếu chữ ký");
        }
        byte[] expected = sign(site, code, ts).getBytes(StandardCharsets.UTF_8);
        // Also accept legacy HMAC over "stationCode:timestamp" for cu-chi 3-part payloads
        byte[] legacyExpected = signLegacy(code, ts).getBytes(StandardCharsets.UTF_8);
        byte[] actual = sig.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        boolean ok = MessageDigest.isEqual(expected, actual)
                || (DEFAULT_SITE.equals(site) && MessageDigest.isEqual(legacyExpected, actual));
        if (!ok) {
            throw new BusinessRuleException("Chữ ký mã QR trạm không hợp lệ");
        }
        if (ts != STATIC_TIMESTAMP) {
            Duration skew = Duration.between(Instant.ofEpochSecond(ts), clock.instant()).abs();
            if (skew.compareTo(Duration.ofMinutes(properties.getQrMaxSkewMinutes())) > 0) {
                throw new BusinessRuleException("Mã QR trạm đã hết hạn");
            }
        }
        return new VerifiedStationQr(site, code);
    }

    /** Backward-compatible: returns station code only. */
    public String verify(String qrPayload) {
        return verifyDetailed(qrPayload).stationCode();
    }

    private static long parseTimestamp(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new BusinessRuleException("Mã QR trạm không hợp lệ");
        }
    }

    private String sign(String siteCode, String stationCode, long timestamp) {
        return hmacHex(siteCode + ":" + stationCode + ":" + timestamp);
    }

    private String signLegacy(String stationCode, long timestamp) {
        return hmacHex(stationCode + ":" + timestamp);
    }

    private String hmacHex(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getQrHmacSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }
}
