package com.histar.be.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @Email String email,
        @NotBlank String password,
        String displayName,
        /** Optional creator referral code (C4) — attributed on signup. */
        String referralCode) {

    public RegisterRequest(String email, String password, String displayName) {
        this(email, password, displayName, null);
    }
}
