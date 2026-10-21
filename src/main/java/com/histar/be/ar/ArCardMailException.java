package com.histar.be.ar;

public final class ArCardMailException extends RuntimeException {
    private final int status;
    private final String code;
    private final Long retryAfterSeconds;
    public ArCardMailException(int status, String code, String message) { this(status, code, message, null); }
    public ArCardMailException(int status, String code, String message, Long retryAfterSeconds) {
        super(message); this.status=status; this.code=code; this.retryAfterSeconds=retryAfterSeconds;
    }
    public int status() { return status; }
    public String code() { return code; }
    public Long retryAfterSeconds() { return retryAfterSeconds; }
}
