package com.histar.be.rag.dto;

import com.histar.be.rag.entity.StationChatPrompt;
import java.util.UUID;

public record ChatPromptResponse(UUID id, String persona, String chipLabel, String questionText, int sortOrder) {

    public static ChatPromptResponse from(StationChatPrompt p) {
        return new ChatPromptResponse(
                p.getId(), p.getPersona(), p.getChipLabel(), p.getQuestionText(), p.getSortOrder());
    }
}
