package com.histar.be.chat.service;

public final class PersonaMapper {

    private PersonaMapper() {}

    public static String resolvePersonaKey(String characterName) {
        if (characterName != null && characterName.toLowerCase().contains("chị năm")) {
            return "chi-nam";
        }
        return "huong-dan-vien";
    }
}
