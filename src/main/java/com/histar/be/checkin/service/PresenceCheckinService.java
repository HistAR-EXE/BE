package com.histar.be.checkin.service;

import com.histar.be.checkin.dto.CheckinRequest;
import com.histar.be.checkin.presence.PresenceInput;
import com.histar.be.checkin.presence.PresenceMethod;
import com.histar.be.checkin.presence.StationQrService;
import com.histar.be.checkin.presence.StationQrService.VerifiedStationQr;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.gamification.dto.CheckinResultDto;
import com.histar.be.gamification.service.GamificationService;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Validates presence evidence (station QR, hints, idempotency key) and delegates to gamification. */
@Service
@RequiredArgsConstructor
public class PresenceCheckinService {

    private final StationQrService stationQrService;
    private final GamificationService gamificationService;
    private final LocationRepository locationRepository;

    public CheckinResultDto checkin(UUID userId, CheckinRequest request) {
        boolean hasQrPayload = request.qrPayload() != null && !request.qrPayload().isBlank();
        boolean hasQrCode = request.qrCode() != null && !request.qrCode().isBlank();
        if (!hasQrPayload && !hasQrCode) {
            throw new BusinessRuleException("Cần qrCode hoặc qrPayload");
        }

        PresenceMethod requested = PresenceMethod.parse(request.presenceMethod());
        if (requested == PresenceMethod.QR && !hasQrPayload) {
            throw new BusinessRuleException("presenceMethod=QR cần qrPayload của trạm");
        }

        String siteCode = null;
        String stationCode = null;
        boolean qrVerified = false;
        UUID locationId = request.locationId();

        if (hasQrPayload) {
            VerifiedStationQr verified = stationQrService.verifyDetailed(request.qrPayload());
            if (request.stationCode() != null
                    && !request.stationCode().isBlank()
                    && !verified.stationCode().equalsIgnoreCase(request.stationCode().trim())) {
                throw new BusinessRuleException("stationCode không khớp mã QR");
            }
            siteCode = verified.siteCode();
            stationCode = verified.stationCode();
            qrVerified = true;
            // Prefer location resolved from site_code on the QR when request location is missing or mismatched.
            Location bySite = locationRepository.findBySiteCodeIgnoreCase(siteCode).orElse(null);
            if (bySite != null) {
                if (locationId == null || !locationId.equals(bySite.getId())) {
                    locationId = bySite.getId();
                }
            }
        } else if (request.stationCode() != null && !request.stationCode().isBlank()) {
            stationCode = StationQrService.normalizeStationCode(request.stationCode());
        }

        if (locationId == null) {
            throw new BusinessRuleException("Thiếu locationId — quét QR trạm có siteCode hoặc chọn địa điểm");
        }

        return gamificationService.processCheckin(
                userId,
                locationId,
                request.latitude(),
                request.longitude(),
                request.qrCode(),
                new PresenceInput(siteCode, stationCode, qrVerified, request.clientUuid()));
    }
}
