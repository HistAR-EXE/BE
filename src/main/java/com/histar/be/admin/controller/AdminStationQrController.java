package com.histar.be.admin.controller;

import com.histar.be.checkin.presence.StationQrService;
import com.histar.be.checkin.presence.StationQrService.SignedStationQr;
import com.histar.be.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStationQrController {

    private final StationQrService stationQrService;

    /**
     * Returns a signed station QR. Default {@code static=true} for waterproof field signs
     * ({@code siteCode:ST0x:0:sig}). Pass {@code static=false} for short-lived staff QR.
     */
    @GetMapping("/{code}/qr-token")
    public ApiResponse<SignedStationQr> qrToken(
            @PathVariable String code,
            @RequestParam(defaultValue = "cu-chi") String siteCode,
            @RequestParam(name = "static", defaultValue = "true") boolean staticQr) {
        if (staticQr) {
            return ApiResponse.ok(stationQrService.issueStatic(siteCode, code));
        }
        return ApiResponse.ok(stationQrService.issue(siteCode, code));
    }
}
