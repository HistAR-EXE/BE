package com.histar.be.common.gamification;

import com.histar.be.common.exception.BusinessRuleException;
import java.util.UUID;

public final class QrPayloadParser {

    private static final String PREFIX_LOCATION = "timelens:location:";
    private static final String PREFIX_SECRET = "timelens:secret:";

    private QrPayloadParser() {}

    public static QrPayload parse(String qrCode) {
        if (qrCode == null || qrCode.isBlank()) {
            throw new BusinessRuleException("Mã QR không hợp lệ");
        }
        String trimmed = qrCode.trim();
        if (trimmed.regionMatches(true, 0, PREFIX_SECRET, 0, PREFIX_SECRET.length())) {
            return new QrPayload(QrPayloadType.SECRET, parseUuid(trimmed.substring(PREFIX_SECRET.length())));
        }
        if (trimmed.regionMatches(true, 0, PREFIX_LOCATION, 0, PREFIX_LOCATION.length())) {
            return new QrPayload(QrPayloadType.LOCATION, parseUuid(trimmed.substring(PREFIX_LOCATION.length())));
        }
        return new QrPayload(QrPayloadType.LOCATION, parseUuid(trimmed));
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("Mã QR không chứa location hợp lệ");
        }
    }
}
