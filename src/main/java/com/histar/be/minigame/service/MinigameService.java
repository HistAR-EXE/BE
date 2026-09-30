package com.histar.be.minigame.service;

import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.minigame.dto.MinigameProgressItem;
import com.histar.be.minigame.dto.MinigameResponse;
import com.histar.be.minigame.dto.MinigameSubmitResponse;
import com.histar.be.minigame.entity.StationMinigame;
import com.histar.be.minigame.entity.StationMinigameScore;
import com.histar.be.minigame.repository.StationMinigameRepository;
import com.histar.be.minigame.repository.StationMinigameScoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MinigameService {

    private final StationMinigameRepository minigameRepository;
    private final StationMinigameScoreRepository scoreRepository;

    public List<MinigameResponse> listForStation(String siteCode, String stationCode, UUID userId) {
        List<StationMinigame> games =
                minigameRepository.findBySiteCodeAndStationCodeOrderBySortOrderAsc(siteCode, stationCode);
        Map<UUID, Integer> bestByGame = userId == null
                ? Map.of()
                : scoreRepository.findByUserIdOrderByCompletedAtDesc(userId).stream()
                        .collect(Collectors.toMap(
                                StationMinigameScore::getMinigameId,
                                StationMinigameScore::getScore,
                                Math::max));
        return games.stream()
                .map(g -> MinigameResponse.from(g, bestByGame.get(g.getId())))
                .toList();
    }

    public StationMinigame requireGame(UUID minigameId) {
        return minigameRepository.findById(minigameId).orElseThrow(() -> new ResourceNotFoundException("Minigame not found"));
    }

    @Transactional
    public MinigameSubmitResponse submit(UUID userId, UUID minigameId, int score) {
        requireGame(minigameId);
        Instant now = Instant.now();
        var existing = scoreRepository.findByUserIdAndMinigameId(userId, minigameId);
        boolean newBest = false;
        StationMinigameScore saved;
        if (existing.isPresent()) {
            StationMinigameScore row = existing.get();
            if (score > row.getScore()) {
                row.setScore(score);
                row.setCompletedAt(now);
                newBest = true;
            }
            saved = scoreRepository.save(row);
        } else {
            saved = scoreRepository.save(StationMinigameScore.builder()
                    .userId(userId)
                    .minigameId(minigameId)
                    .score(score)
                    .completedAt(now)
                    .build());
            newBest = true;
        }
        return MinigameSubmitResponse.builder()
                .minigameId(minigameId)
                .score(saved.getScore())
                .completedAt(saved.getCompletedAt())
                .newBest(newBest)
                .build();
    }

    public List<MinigameProgressItem> progressForUser(UUID userId) {
        List<StationMinigameScore> scores = scoreRepository.findByUserIdOrderByCompletedAtDesc(userId);
        if (scores.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = scores.stream().map(StationMinigameScore::getMinigameId).distinct().toList();
        Map<UUID, StationMinigame> games = minigameRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(StationMinigame::getId, Function.identity()));
        return scores.stream()
                .map(s -> {
                    StationMinigame g = games.get(s.getMinigameId());
                    if (g == null) {
                        return null;
                    }
                    return MinigameProgressItem.builder()
                            .minigameId(g.getId())
                            .siteCode(g.getSiteCode())
                            .stationCode(g.getStationCode())
                            .gameType(g.getGameType())
                            .title(g.getTitle())
                            .score(s.getScore())
                            .completedAt(s.getCompletedAt())
                            .build();
                })
                .filter(item -> item != null)
                .toList();
    }
}
