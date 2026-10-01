package com.histar.be.auth.dto;

public record PasswordResetRequestResponse(String message, String debugOtp) {

    public PasswordResetRequestResponse(String message) {
        this(message, null);
    }
}
