package com.histar.be.common.exception;

public class LmsPremiumRequiredException extends BaseException {

    public LmsPremiumRequiredException(String message) {
        super(ErrorCode.LMS_PREMIUM_REQUIRED, message);
    }
}
