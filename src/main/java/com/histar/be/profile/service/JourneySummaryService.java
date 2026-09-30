package com.histar.be.profile.service;

import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.minigame.entity.StationMinigame;
import com.histar.be.minigame.entity.StationMinigameScore;
import com.histar.be.minigame.repository.StationMinigameRepository;
import com.histar.be.minigame.repository.StationMinigameScoreRepository;
import com.histar.be.profile.dto.JourneySummaryResponse;
import com.histar.be.profile.entity.Profile;
import com.histar.be.story.repository.StoryChapterRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JourneySummaryService {

    static final String DEFAULT_SITE = "cu-chi";

    private final ProfileService profileService;
    private final CheckinRepository checkinRepository;
    private final StoryChapterRepository storyChapterRepository;
    private final StationMinigameScoreRepository scoreRepository;
    private final StationMinigameRepository minigameRepository;
    private final LocationRepository locationRepository;

    @Transactional(readOnly = true)
    public JourneySummaryResponse summarize(UUID userId) {
        return summarize(userId, DEFAULT_SITE);
    }

    @Transactional(readOnly = true)
    public JourneySummaryResponse summarize(UUID userId, String siteCodeRaw) {
        String siteCode = siteCodeRaw == null || siteCodeRaw.isBlank()
                ? DEFAULT_SITE
                : siteCodeRaw.trim().toLowerCase(Locale.ROOT);
        Profile profile = profileService.findById(userId);

        UUID locationId = locationRepository
                .findBySiteCodeIgnoreCase(siteCode)
                .map(loc -> loc.getId())
                .orElse(null);

        List<Checkin> checkins = checkinRepository.findByUserIdOrderByCreatedAtDesc(userId);
        TreeSet<String> stations = new TreeSet<>();
        Instant first = null;
        Instant last = null;
        for (Checkin c : checkins) {
            if (locationId != null && c.getLocationId() != null && !locationId.equals(c.getLocationId())) {
                continue;
            }
            if (c.getStationCode() != null && !c.getStationCode().isBlank()) {
                stations.add(c.getStationCode().trim().toUpperCase(Locale.ROOT));
            }
            Instant at = c.getCreatedAt();
            if (at != null) {
                if (first == null || at.isBefore(first)) {
                    first = at;
                }
                if (last == null || at.isAfter(last)) {
                    last = at;
                }
            }
        }

        var chapters = storyChapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc(siteCode);
        int totalStations = chapters.size();
        int chaptersCompleted = (int) chapters.stream()
                .filter(ch -> ch.getStationCode() != null
                        && stations.contains(ch.getStationCode().trim().toUpperCase(Locale.ROOT)))
                .count();

        List<StationMinigameScore> scores = scoreRepository.findByUserIdOrderByCompletedAtDesc(userId);
        // Filter scores to games belonging to this site when possible
        Map<UUID, StationMinigame> gameById = minigameRepository.findAll().stream()
                .filter(g -> siteCode.equalsIgnoreCase(g.getSiteCode()))
                .collect(Collectors.toMap(StationMinigame::getId, Function.identity(), (a, b) -> a));
        List<StationMinigameScore> siteScores = scores.stream()
                .filter(s -> gameById.containsKey(s.getMinigameId()))
                .toList();
        int played = siteScores.size();
        int avg = played == 0
                ? 0
                : (int) Math.round(siteScores.stream().mapToInt(StationMinigameScore::getScore).average().orElse(0));
        StationMinigameScore best =
                siteScores.stream().max(Comparator.comparingInt(StationMinigameScore::getScore)).orElse(null);
        String bestTitle = best != null && gameById.get(best.getMinigameId()) != null
                ? gameById.get(best.getMinigameId()).getTitle()
                : null;

        String name = profile.getDisplayName() != null && !profile.getDisplayName().isBlank()
                ? profile.getDisplayName()
                : "Du kích HistAR";
        String siteLabel = switch (siteCode) {
            case "hoang-thanh-thang-long" -> "Hoàng thành Thăng Long";
            case "dai-noi-hue" -> "Đại Nội Huế";
            default -> "địa đạo Củ Chi";
        };
        String headline = stations.isEmpty()
                ? "Hành trình của bạn sắp bắt đầu"
                : "Bạn đã khám phá " + stations.size() + " trạm tại " + siteLabel;

        return new JourneySummaryResponse(
                name,
                profile.getLevel() != null ? profile.getLevel() : 1,
                profile.getTotalPoints() != null ? profile.getTotalPoints() : 0,
                stations.size(),
                totalStations,
                chaptersCompleted,
                played,
                avg,
                best != null ? best.getScore() : 0,
                bestTitle,
                first,
                last,
                List.copyOf(stations),
                headline);
    }
}
