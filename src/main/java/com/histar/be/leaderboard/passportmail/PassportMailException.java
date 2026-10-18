package com.histar.be.leaderboard.passportmail;

public class PassportMailException extends RuntimeException {
    private final int status;
    private final String code;
    private final Long retryAfterSeconds;
    public PassportMailException(int status, String code, String message) {
        this(status, code, message, null);
    }
    public PassportMailException(int status, String code, String message, Long retryAfterSeconds) {
        super(message); this.status=status; this.code=code; this.retryAfterSeconds=retryAfterSeconds;
    }
    public int getStatus() { return status; }
    public String getCode() { return code; }
    public Long getRetryAfterSeconds() { return retryAfterSeconds; }
}
