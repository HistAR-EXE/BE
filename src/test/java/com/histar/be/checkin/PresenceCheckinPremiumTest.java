package com.histar.be.checkin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.histar.be.checkin.dto.CheckinRequest;
import com.histar.be.checkin.presence.StationQrService;
import com.histar.be.checkin.presence.StationQrService.VerifiedStationQr;
import com.histar.be.checkin.service.PresenceCheckinService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.profile.service.TierAccessService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PresenceCheckinPremiumTest {
    private final StationQrService qr = mock(StationQrService.class);
    private final GamificationService gamification = mock(GamificationService.class);
    private final LocationRepository locations = mock(LocationRepository.class);
    private final TierAccessService access = mock(TierAccessService.class);
    private final PresenceCheckinService service = new PresenceCheckinService(qr, gamification, locations, access);
    private final UUID userId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();

    @Test
    void freeUserCanScanFirstTwoStations() {
        when(qr.verifyDetailed("qr-st02")).thenReturn(new VerifiedStationQr("cu-chi", "ST02"));
        CheckinRequest request = new CheckinRequest(locationId, null, null, null, "ST02", "qr-st02", "QR", null);
        assertDoesNotThrow(() -> service.checkin(userId, request));
        verify(gamification).processCheckin(eq(userId), eq(locationId), any(), any(), any(), any());
    }

    @Test
    void freeUserCannotScanPremiumStationEvenWithValidQr() {
        when(qr.verifyDetailed("qr-st03")).thenReturn(new VerifiedStationQr("cu-chi", "ST03"));
        CheckinRequest request = new CheckinRequest(locationId, null, null, null, "ST03", "qr-st03", "QR", null);
        assertThrows(BusinessRuleException.class, () -> service.checkin(userId, request));
        verifyNoInteractions(gamification);
    }

    @Test
    void premiumOrJourneyPassCanScanPremiumStation() {
        when(qr.verifyDetailed("qr-st06")).thenReturn(new VerifiedStationQr("cu-chi", "ST06"));
        when(access.hasStoryAccess(userId, "cu-chi")).thenReturn(true);
        CheckinRequest request = new CheckinRequest(locationId, null, null, null, "ST06", "qr-st06", "QR", null);
        assertDoesNotThrow(() -> service.checkin(userId, request));
        verify(gamification).processCheckin(eq(userId), eq(locationId), any(), any(), any(), any());
    }

    @Test
    void locationQrCannotBypassPremiumStationGate() {
        Location location = Location.builder().siteCode("cu-chi").build();
        when(locations.findById(locationId)).thenReturn(Optional.of(location));
        CheckinRequest request = new CheckinRequest(locationId, 11.0, 106.0, "timelens:location:" + locationId,
                "ST04", null, "GPS", null);
        assertThrows(BusinessRuleException.class, () -> service.checkin(userId, request));
        verifyNoInteractions(gamification);
    }
}
