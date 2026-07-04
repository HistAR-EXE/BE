package com.histar.be.discovery.binding.dto;

import java.util.UUID;

public record DiscoveryBindingResponse(
        String unlockKey,
        String recordKey,
        String engagement,
        String hrefTemplate,
        int sortOrder,
        UUID artifactId,
        int xpBonus,
        UUID questStepId) {}
