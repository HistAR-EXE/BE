package com.histar.be.minigame.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.minigame.entity.StationMinigame;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

/**
 * config là Map thường (không phải JsonNode của Jackson 2): bộ chuyển JSON của Spring Boot 4 (Jackson 3)
 * không coi JsonNode Jackson 2 là cây JSON và sẽ in ra các thuộc tính getter (array, nodeType…) thay cho nội dung.
 *
 * <p>Mini game của trạm chỉ mở khi người chơi đã check-in tại chính trạm đó (quét QR). Khi còn khoá,
 * {@code config} rỗng để câu hỏi và đáp án không lộ ra ngoài.
 */
@Builder
public record MinigameResponse(
        UUID id,
        String siteCode,
        String stationCode,
        String gameType,
        String title,
        Map<String, Object> config,
        int sortOrder,
        Integer bestScore,
        boolean unlocked,
        String lockReason) {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    public static MinigameResponse from(StationMinigame game, Integer bestScore, boolean unlocked, String lockReason) {
        Map<String, Object> config = new LinkedHashMap<>();
        if (unlocked) {
            try {
                config = MAPPER.readValue(game.getConfigJson(), MAP_TYPE);
            } catch (Exception e) {
                config = new LinkedHashMap<>();
            }
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
                .unlocked(unlocked)
                .lockReason(lockReason)
                .build();
    }
}
