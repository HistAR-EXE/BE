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
    private final Instant timestamp;

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
