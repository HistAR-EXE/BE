package com.histar.be.chat.service;

public final class PersonaMapper {

    private PersonaMapper() {}

    /**
     * Chat now has one system assistant.  Keep the parameter for callers that
     * still pass an older character name, but never use it to select a prompt
     * or an LLM persona.
     */
    public static String resolvePersonaKey(String characterName) {
        return "chrono";
    }
}
