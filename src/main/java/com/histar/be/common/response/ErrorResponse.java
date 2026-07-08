package com.histar.be.common.response;

import com.histar.be.common.exception.ErrorCode;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final String upgradeUrl;
    private final String type;
    private final String upgradePackage;
    private final Instant timestamp;

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse ofQuota(ErrorCode errorCode, String message, String upgradeUrl) {
        return ofQuota(errorCode, message, upgradeUrl, "B2C_DAILY", null);
    }

    public static ErrorResponse ofQuota(
            ErrorCode errorCode,
            String message,
            String upgradeUrl,
            String type,
            String upgradePackage) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .upgradeUrl(upgradeUrl)
                .type(type)
                .upgradePackage(upgradePackage)
                .timestamp(Instant.now())
                .build();
    }
}
