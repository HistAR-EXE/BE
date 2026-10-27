package com.histar.be.profile.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Own-account totals and entitlement provenance; site-scoped progress lives in its own API. */
public record ProfileOverviewResponse(
        ProfileMeResponse profile,
        Account account,
        Stats stats,
        List<Stamp> passportStamps,
        List<HeritageLocation> locations,
        Instant generatedAt) {
    public record Account(Instant createdAt, String provider, String personalTier,
            boolean organizationPremium, String organizationStatus, LocalDate organizationExpiresAt) {}
    public record Stats(long completedQuests, long discoveredLocations, long checkedInLocations,
            long passportStamps, long earnedBadges) {}
    public record Stamp(UUID locationId, String locationName, String siteCode, String coverImage,
            Instant completedAt, boolean arVerified) {}
    public record HeritageLocation(UUID id, String name, String siteCode, String coverImage) {}
}
