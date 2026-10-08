package com.histar.be.quest.minigame;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record MinigameViewResponse(UUID id, String engine, String title, String levelId, JsonNode view) {}
