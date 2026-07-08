package com.histar.be.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    CONFLICT("CONFLICT"),
    NOT_FOUND("NOT_FOUND"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    BUSINESS_RULE("BUSINESS_RULE"),
    EMAIL_NOT_VERIFIED("EMAIL_NOT_VERIFIED"),
    QUOTA_EXCEEDED("QUOTA_EXCEEDED"),
    TRIAL_EXPIRED("TRIAL_EXPIRED"),
    LMS_PREMIUM_REQUIRED("LMS_PREMIUM_REQUIRED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;
}
