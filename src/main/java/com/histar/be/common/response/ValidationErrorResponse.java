package com.histar.be.common.response;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ValidationErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final Map<String, String> fieldErrors;
    private final Instant timestamp;
}
