package com.histar.be.story.dto;

import java.util.UUID;

/**
 * {@code synopsis} is withheld (null) while the chapter is locked. {@code lockReason} is {@code SEQUENCE}
 * (previous chapter not completed) or {@code PREMIUM}; null when unlocked.
 */
public record StoryChapterResponse(
        UUID id,
        String siteCode,
        int chapterNumber,
        String stationCode,
        String title,
        String synopsis,
        boolean requiresPremium,
        int sortOrder,
        boolean unlocked,
        boolean completed,
        String lockReason) {}