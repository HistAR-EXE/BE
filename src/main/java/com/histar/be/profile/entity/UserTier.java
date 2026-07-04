package com.histar.be.profile.entity;

public enum UserTier {
    FREE,
    PREMIUM;

    public static UserTier fromStored(String value) {
        if (value == null || value.isBlank()) {
            return FREE;
        }
        try {
            return UserTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return FREE;
        }
    }
}
