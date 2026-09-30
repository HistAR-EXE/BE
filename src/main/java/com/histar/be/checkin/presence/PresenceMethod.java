package com.histar.be.checkin.presence;

import com.histar.be.common.exception.BusinessRuleException;

public enum PresenceMethod {
    QR,
    GPS,
    MANUAL;

    public static PresenceMethod parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PresenceMethod.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleException("presenceMethod không hợp lệ (QR|GPS|MANUAL)");
        }
    }
}
