package com.histar.be.common.exception;

public class TrialExpiredException extends BaseException {

    public TrialExpiredException(String message) {
        super(ErrorCode.TRIAL_EXPIRED, message);
    }
}
