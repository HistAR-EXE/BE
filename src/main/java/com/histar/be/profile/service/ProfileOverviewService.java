package com.histar.be.profile.service;

import com.histar.be.badge.user.repository.UserBadgeRepository;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import com.histar.be.profile.dto.PassportStampResponse;
import com.histar.be.profile.dto.ProfileOverviewResponse;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.entity.UserRole;
import com.histar.be.profile.entity.UserTier;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileOverviewService {
    private final ProfileService profiles;
    private final ProfileMeService views;
    private final PassportService passports;
    private final UserQuestProgressRepository quests;
    private final UserDiscoveryRepository discoveries;
    private final CheckinRepository checkins;
    private final UserBadgeRepository badges;
    private final LocationRepository locations;
    private final OrganizationEntitlementService entitlements;
    private final OrganizationRepository organizations;
    private final B2cSubscriptionRepository subscriptions;

    @Transactional(readOnly = true)
    public ProfileOverviewResponse overview(UUID userId) {
        Profile profile = profiles.findById(userId);
        String personalTier = personalTier(profile);
        boolean organizationPremium = entitlements.hasActiveMembership(profile);
        String effectiveTier = "PREMIUM".equals(personalTier) || organizationPremium
                || UserRole.ADMIN == UserRole.fromStored(profile.getRole()) ? "PREMIUM" : "FREE";
        var ownProfile = views.build(profile).withTier(effectiveTier);
        var organization = profile.getOrgId() == null ? null : organizations.findById(profile.getOrgId()).orElse(null);
        // Complete catalog, never a paginated first fifty masquerading as all locations.
        List<Location> catalog = locations.findAll();
        Map<UUID, Location> byId = catalog.stream().collect(Collectors.toMap(
                Location::getId, Function.identity(), (first, duplicate) -> first));
        Set<UUID> verifiedLocations = checkins.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(c -> "QR".equalsIgnoreCase(c.getPresenceMethod())
                        && c.getStationCode() != null && !c.getStationCode().isBlank())
                .map(c -> c.getLocationId()).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, PassportStampResponse> stampsByLocation = new LinkedHashMap<>();
        for (PassportStampResponse stamp : passports.getPassport(userId).stamps()) {
            if (stamp.locationId() == null) continue;
            stampsByLocation.merge(stamp.locationId(), stamp, (first, next) ->
                    first.completedAt() == null || (next.completedAt() != null && next.completedAt().isAfter(first.completedAt()))
                            ? next : first);
        }
        List<ProfileOverviewResponse.Stamp> stamps = stampsByLocation.values().stream().map(stamp -> {
            Location location = byId.get(stamp.locationId());
            String name = location == null ? stamp.locationName() : location.getName();
            return new ProfileOverviewResponse.Stamp(stamp.locationId(), name == null || name.isBlank() ? "Di tích chưa có thông tin" : name,
                    location == null ? null : location.getSiteCode(), location == null ? null : location.getCoverImage(),
                    stamp.completedAt(), verifiedLocations.contains(stamp.locationId()));
        }).toList();
        long discoveredLocations = discoveries.findDistinctLocationIdsByUserId(userId).stream()
                .filter(Objects::nonNull).distinct().count();
        return new ProfileOverviewResponse(ownProfile,
                new ProfileOverviewResponse.Account(profile.getCreatedAt(), profile.getProvider(), personalTier,
                        organizationPremium, organization == null ? null : organization.getStatus(),
                        organization == null ? null : organization.getPlanEndDate()),
                new ProfileOverviewResponse.Stats(quests.countDistinctCompletedQuests(userId), discoveredLocations,
                        checkins.countDistinctLocationsByUserId(userId), stamps.size(), badges.countDistinctEarnedBadges(userId)),
                stamps, catalog.stream().map(location -> new ProfileOverviewResponse.HeritageLocation(location.getId(),
                        location.getName(), location.getSiteCode(), location.getCoverImage())).toList(), Instant.now());
    }

    private String personalTier(Profile profile) {
        var active = subscriptions.findByUserIdAndIsActiveTrue(profile.getId());
        if (active.isPresent()) {
            var subscription = active.get();
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            boolean current = (subscription.getStartDate() == null || !subscription.getStartDate().isAfter(today))
                    && (subscription.getEndDate() == null || !subscription.getEndDate().isBefore(today));
            return current ? "PREMIUM" : "FREE";
        }
        // Legacy lifetime/manual personal grants are retained; organization rights never set this column.
        return UserTier.fromStored(profile.getTier()).name();
    }
}
