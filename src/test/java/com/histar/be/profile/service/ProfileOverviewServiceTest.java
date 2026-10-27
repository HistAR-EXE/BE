package com.histar.be.profile.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.badge.user.repository.UserBadgeRepository;
import com.histar.be.billing.entity.B2cSubscription;
import com.histar.be.billing.repository.B2cSubscriptionRepository;
import com.histar.be.checkin.entity.Checkin;
import com.histar.be.checkin.repository.CheckinRepository;
import com.histar.be.discovery.repository.UserDiscoveryRepository;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.organization.service.OrganizationEntitlementService;
import com.histar.be.organization.repository.OrganizationRepository;
import com.histar.be.profile.dto.*;
import com.histar.be.profile.entity.Profile;
import com.histar.be.quest.progress.repository.UserQuestProgressRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileOverviewServiceTest {
    @Mock ProfileService profiles;
    @Mock ProfileMeService views;
    @Mock PassportService passports;
    @Mock UserQuestProgressRepository quests;
    @Mock UserDiscoveryRepository discoveries;
    @Mock CheckinRepository checkins;
    @Mock UserBadgeRepository badges;
    @Mock LocationRepository locations;
    @Mock OrganizationEntitlementService entitlements;
    @Mock OrganizationRepository organizations;
    @Mock B2cSubscriptionRepository subscriptions;
    @InjectMocks ProfileOverviewService service;
    UUID userId = UUID.randomUUID();
    Profile profile;
    @BeforeEach void setup() {
        profile = Profile.builder().id(userId).email("profile@example.test").role("USER").tier("FREE")
                .provider("local").createdAt(Instant.parse("2026-10-01T00:00:00Z")).build();
        when(profiles.findById(userId)).thenReturn(profile);
        when(views.build(profile)).thenReturn(ProfileMeResponse.from(profile,
                new com.histar.be.config.GamificationProperties(), null, null, "NONE", null, List.of()));
        when(passports.getPassport(userId)).thenReturn(new PassportResponse(List.of()));
    }
    @Test void globalCountsAreNotCappedByFirstFiftyLocationsOrInferredFromBadges() {
        when(quests.countDistinctCompletedQuests(userId)).thenReturn(62L);
        when(discoveries.findDistinctLocationIdsByUserId(userId)).thenReturn(List.of(UUID.randomUUID(), UUID.randomUUID()));
        when(checkins.countDistinctLocationsByUserId(userId)).thenReturn(1L);
        when(badges.countDistinctEarnedBadges(userId)).thenReturn(7L);
        var result = service.overview(userId);
        assertThat(result.stats().completedQuests()).isEqualTo(62);
        assertThat(result.stats().discoveredLocations()).isEqualTo(2);
        assertThat(result.stats().checkedInLocations()).isEqualTo(1);
        assertThat(result.stats().earnedBadges()).isEqualTo(7);
        assertThat(result.account().provider()).isEqualTo("local");
        assertThat(result.account().createdAt()).isEqualTo(profile.getCreatedAt());
        verify(quests).countDistinctCompletedQuests(userId);
    }
    @Test void passportRetainsBeyondFiftyAndMissingLocationsAndUsesStampDate() {
        UUID target = UUID.randomUUID(), deleted = UUID.randomUUID();
        Instant stampTime = Instant.parse("2026-10-20T09:00:00Z");
        var catalog = new ArrayList<Location>();
        for (int i=0;i<60;i++) catalog.add(Location.builder().id(UUID.randomUUID()).name("Location "+i).build());
        catalog.add(Location.builder().id(target).name("Real location").siteCode("cu-chi").build());
        when(locations.findAll()).thenReturn(catalog);
        when(passports.getPassport(userId)).thenReturn(new PassportResponse(List.of(
                new PassportStampResponse(target,"Real location",stampTime,true),
                new PassportStampResponse(deleted,"Archived heritage",stampTime,false))));
        when(checkins.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(
                Checkin.builder().locationId(target).presenceMethod("QR").stationCode("ST01").build()));
        var result = service.overview(userId);
        assertThat(result.locations()).hasSize(61);
        assertThat(result.passportStamps()).hasSize(2);
        assertThat(result.passportStamps().get(0).completedAt()).isEqualTo(stampTime);
        assertThat(result.passportStamps().get(0).arVerified()).isTrue();
        assertThat(result.passportStamps().get(1).locationName()).isEqualTo("Archived heritage");
    }
    @Test void organizationPremiumIsSeparateFromPurchasedTierAndDoesNotVerifyArStamps() {
        when(entitlements.hasActiveMembership(profile)).thenReturn(true);
        UUID loc = UUID.randomUUID();
        when(passports.getPassport(userId)).thenReturn(new PassportResponse(List.of(
                new PassportStampResponse(loc,"Online completed",Instant.now(),true))));
        var result = service.overview(userId);
        assertThat(result.account().personalTier()).isEqualTo("FREE");
        assertThat(result.account().organizationPremium()).isTrue();
        assertThat(result.profile().tier()).isEqualTo("PREMIUM");
        assertThat(result.profile().id()).isEqualTo(userId);
        assertThat(result.passportStamps().get(0).arVerified()).isFalse();
        assertThat(profile.getTier()).isEqualTo("FREE");
    }
    @Test void expiredPurchasedSubscriptionIsNotReportedAsActivePersonalPremium() {
        profile.setTier("PREMIUM");
        when(subscriptions.findByUserIdAndIsActiveTrue(userId)).thenReturn(Optional.of(B2cSubscription.builder()
                .endDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).minusDays(1)).isActive(true).build()));
        var result = service.overview(userId);
        assertThat(result.account().personalTier()).isEqualTo("FREE");
        assertThat(result.profile().tier()).isEqualTo("FREE");
        assertThat(profile.getTier()).isEqualTo("PREMIUM");
    }
}
