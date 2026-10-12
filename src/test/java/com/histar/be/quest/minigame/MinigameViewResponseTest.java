package com.histar.be.quest.minigame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MinigameViewResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void exposesPlayableFieldsInsteadOfJsonNodeImplementationMetadata() throws Exception {
        ObjectNode level = (ObjectNode) mapper.readTree("{\"prompt\":\"Chạm nắp hầm\",\"imageUrl\":\"/minigames/soi-nap-1.svg\"}");

        MinigameViewResponse response = MinigameViewResponse.from(
                UUID.randomUUID(), "soi_nap", "Soi nắp", "lid-1", level, mapper);

        assertEquals("Chạm nắp hầm", response.view().get("prompt"));
        assertEquals("/minigames/soi-nap-1.svg", response.view().get("imageUrl"));
        assertFalse(response.view().containsKey("nodeType"));
    }
}
