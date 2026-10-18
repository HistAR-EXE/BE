package com.histar.be.common.exception;

import java.time.Instant;
import lombok.Getter;

@Getter
public class TrialExpiredException extends BaseException {

    private final Instant expiredAt;

    public TrialExpiredException(String message) {
        this(message, null);
    }

    public TrialExpiredException(String message, Instant expiredAt) {
        super(ErrorCode.TRIAL_EXPIRED, message);
        this.expiredAt = expiredAt;
    }
}
