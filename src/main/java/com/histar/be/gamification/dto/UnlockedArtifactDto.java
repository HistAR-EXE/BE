package com.histar.be.gamification.dto;

import java.util.UUID;

public record UnlockedArtifactDto(UUID id, String name, String imageUrl, String unlockKey) {}
