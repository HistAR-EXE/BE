package com.histar.be.auth.dto;

public record VerifyEmailStatusResponse(
        boolean emailVerified, String message, String debugToken) {

    public VerifyEmailStatusResponse(boolean emailVerified, String message) {
        this(emailVerified, message, null);
    }
}
