package com.histar.be.quest.minigame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;

/**
 * `JsonNode` from Jackson 2 is not recognised as a JSON tree by Spring Boot 4's
 * HTTP mapper. Expose a plain map so clients receive the playable public fields.
 */
public record MinigameViewResponse(UUID id, String engine, String title, String levelId, Map<String, Object> view) {
    public static MinigameViewResponse from(UUID id, String engine, String title, String levelId, JsonNode view, ObjectMapper mapper) {
        return new MinigameViewResponse(id, engine, title, levelId, mapper.convertValue(view, Map.class));
    }
}
