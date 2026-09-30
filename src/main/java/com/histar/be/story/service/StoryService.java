package com.histar.be.story.service;

import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.story.dto.StoryChapterResponse;
import com.histar.be.story.entity.StoryChapter;
import com.histar.be.story.repository.StoryChapterRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoryService {

    public static final String LOCK_SEQUENCE = "SEQUENCE";
    public static final String LOCK_PREMIUM = "PREMIUM";

    private final StoryChapterRepository chapterRepository;
    private final TierAccessService tierAccessService;
    private final CheckinRepository checkinRepository;
    private final LocationRepository locationRepository;

    /**
     * Sequential unlock: chapter N is unlocked only when chapter N-1 is completed (a check-in exists for its
     * station at this site's location) AND the chapter is free or the user has premium / journey pass for the site.
     */
    @Transactional(readOnly = true)
    public List<StoryChapterResponse> listChapters(String siteCode, UUID userId) {
        String site = siteCode == null ? "" : siteCode.trim().toLowerCase(Locale.ROOT);
        boolean premium = userId != null && tierAccessService.hasStoryAccess(userId, site);
        Set<String> completedStations =
                userId == null ? Set.of() : completedStationCodes(userId, site);
        List<StoryChapter> chapters = chapterRepository.findBySiteCodeOrderBySortOrderAscChapterNumberAsc(site);

        List<StoryChapterResponse> result = new ArrayList<>(chapters.size());
        boolean previousCompleted = true;
        for (StoryChapter c : chapters) {
            boolean completed = completedStations.contains(normalize(c.getStationCode()));
            String lockReason = null;
            if (!previousCompleted) {
                lockReason = LOCK_SEQUENCE;
            } else if (c.isRequiresPremium() && !premium) {
                lockReason = LOCK_PREMIUM;
            }
            result.add(toResponse(c, lockReason == null, completed, lockReason));
            previousCompleted = completed;
        }
        return result;
    }

    private Set<String> completedStationCodes(UUID userId, String siteCode) {
        Set<String> codes = new HashSet<>();
        UUID locationId = locationRepository
                .findBySiteCodeIgnoreCase(siteCode)
                .map(loc -> loc.getId())
                .orElse(null);
        List<String> raw = locationId == null
                ? checkinRepository.findDistinctStationCodesByUserId(userId)
                : checkinRepository.findDistinctStationCodesByUserIdAndLocationId(userId, locationId);
        for (String code : raw) {
            String n = normalize(code);
            if (n != null) {
                codes.add(n);
            }
        }
        return codes;
    }

    private static String normalize(String stationCode) {
        return stationCode == null || stationCode.isBlank() ? null : stationCode.trim().toUpperCase(Locale.ROOT);
    }

    private static StoryChapterResponse toResponse(
            StoryChapter c, boolean unlocked, boolean completed, String lockReason) {
        return new StoryChapterResponse(
                c.getId(),
                c.getSiteCode(),
                c.getChapterNumber(),
                c.getStationCode(),
                c.getTitle(),
                unlocked ? c.getSynopsis() : null,
                c.isRequiresPremium(),
                c.getSortOrder(),
                unlocked,
                completed,
                lockReason);
    }
}
