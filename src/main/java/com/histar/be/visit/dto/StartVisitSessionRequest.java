package com.histar.be.visit.dto;

import java.util.UUID;

public record StartVisitSessionRequest(
        UUID locationId,
        String mode,
        String personaGoal,     // "study" | "travel" | "research"
        String sessionDuration, // "15" | "30" | "60"
        String aiTone           // "heritage" | "modern"
) {}