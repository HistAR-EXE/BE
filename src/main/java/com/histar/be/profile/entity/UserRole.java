package com.histar.be.profile.entity;

public enum UserRole {
    USER,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }

    public static UserRole fromStored(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return USER;
        }
    }
}
