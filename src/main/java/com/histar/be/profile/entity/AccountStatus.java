package com.histar.be.profile.entity;

public enum AccountStatus {
    ACTIVE,
    SUSPENDED;

    public static AccountStatus fromStored(String value) {
        if (value == null || value.isBlank()) {
            return ACTIVE;
        }
        try {
            return AccountStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ACTIVE;
        }
    }

    public boolean isSuspended() {
        return this == SUSPENDED;
    }
}
