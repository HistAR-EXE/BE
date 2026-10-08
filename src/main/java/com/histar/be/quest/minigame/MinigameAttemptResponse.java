package com.histar.be.quest.minigame;

public record MinigameAttemptResponse(
        int stars,
        boolean passed,
        String learnFact,
        String learnUrl,
        boolean discoveryRecorded,
        int discoveryXp,
        int attempts,
        boolean pending) {}
