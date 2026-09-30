package com.histar.be.minigame.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.minigame.entity.StationMinigame;
import java.util.UUID;
import lombok.Builder;

@Builder
public record MinigameResponse(
        UUID id,
        String siteCode,
        String stationCode,
        String gameType,
        String title,
        JsonNode config,
        int sortOrder,
        Integer bestScore) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static MinigameResponse from(StationMinigame game, Integer bestScore) {
        JsonNode config;
        try {
            config = MAPPER.readTree(game.getConfigJson());
        } catch (Exception e) {
            config = MAPPER.createObjectNode();
        }
        return MinigameResponse.builder()
                .id(game.getId())
                .siteCode(game.getSiteCode())
                .stationCode(game.getStationCode())
                .gameType(game.getGameType())
                .title(game.getTitle())
                .config(config)
                .sortOrder(game.getSortOrder())
                .bestScore(bestScore)
                .build();
    }
}
