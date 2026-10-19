package com.histar.be.minigame.service;

import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.minigame.dto.MinigameProgressItem;
import com.histar.be.minigame.dto.MinigameResponse;
import com.histar.be.minigame.dto.MinigameSubmitResponse;
import com.histar.be.minigame.entity.StationMinigame;
import com.histar.be.minigame.entity.StationMinigameScore;
import com.histar.be.minigame.repository.StationMinigameRepository;
import com.histar.be.minigame.repository.StationMinigameScoreRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
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

    /** Mini game của trạm còn khoá vì người chơi chưa check-in (quét QR) tại chính trạm đó. */
    public static final String LOCK_CHECKIN = "CHECKIN";

    private final StationMinigameRepository minigameRepository;
    private final StationMinigameScoreRepository scoreRepository;
    private final CheckinRepository checkinRepository;
    private final LocationRepository locationRepository;

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
                .map(g -> {
                    boolean unlocked = checkedIn(userId, g.getSiteCode(), g.getStationCode());
                    return MinigameResponse.from(g, bestByGame.get(g.getId()), unlocked, unlocked ? null : LOCK_CHECKIN);
                })
                .toList();
    }

    /**
     * Đã quét QR tại trạm này chưa. Điểm đã lưu cũng mở khoá: người chơi từng check-in rồi chơi xong
     * vẫn xem lại được, kể cả khi dữ liệu check-in cũ bị dọn.
     */
    public boolean checkedIn(UUID userId, String siteCode, String stationCode) {
        if (userId == null || stationCode == null || stationCode.isBlank()) {
            return false;
        }
        UUID locationId = locationRepository
                .findBySiteCodeIgnoreCase(siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT))
                .map(loc -> loc.getId())
                .orElse(null);
        if (locationId == null) {
            return false;
        }
        return checkinRepository.existsByUserIdAndLocationIdAndStationCodeIgnoreCase(
                userId, locationId, stationCode.trim());
    }

    public StationMinigame requireGame(UUID minigameId) {
        return minigameRepository.findById(minigameId).orElseThrow(() -> new ResourceNotFoundException("Minigame not found"));
    }

    @Transactional
    public MinigameSubmitResponse submit(UUID userId, UUID minigameId, int score) {
        StationMinigame game = requireGame(minigameId);
        if (!checkedIn(userId, game.getSiteCode(), game.getStationCode())) {
            throw new BusinessRuleException(
                    "Mini game trạm " + game.getStationCode() + " chỉ mở sau khi check-in tại trạm");
        }
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
