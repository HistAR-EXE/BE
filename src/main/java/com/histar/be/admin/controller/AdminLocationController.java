package com.histar.be.admin.controller;

import com.histar.be.admin.audit.AdminAuditCommand;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.admin.dto.LocationArToggleRequest;
import com.histar.be.admin.dto.LocationEraCountResponse;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.location.entity.Location;
import com.histar.be.location.repository.LocationRepository;
import com.histar.be.location.service.LocationEraValidationService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/locations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLocationController {

    private final LocationEraValidationService locationEraValidationService;
    private final LocationRepository locationRepository;
    private final AdminAuditService adminAuditService;
    private final CurrentUserAccessor currentUser;

    @GetMapping("/{id}/era-count")
    public ApiResponse<LocationEraCountResponse> eraCount(@PathVariable UUID id) {
        int count = locationEraValidationService.countDistinctEras(id);
        return ApiResponse.ok(new LocationEraCountResponse(count, count >= 3));
    }

    /**
     * Toggle AR availability for a location.
     * Atomically updates is_ar_available and appends an audit log entry.
     * Admin UI reacts immediately; FE reads from BE on next map load.
     */
    @PatchMapping("/{id}/ar-availability")
    public ApiResponse<Void> toggleArAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody LocationArToggleRequest request) {
        UUID actorId = currentUser.getUserId()
                .orElseThrow(() -> new AuthException("Unauthorized"));

        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));

        boolean oldValue = Boolean.TRUE.equals(location.getIsArAvailable());
        location.setIsArAvailable(request.isArAvailable());
        locationRepository.save(location);

        adminAuditService.append(new AdminAuditCommand(
                actorId,
                "LOCATION",
                id,
                request.isArAvailable() ? "AR_ENABLED" : "AR_DISABLED",
                null,
                null,
                request.reason(),
                null,
                Map.of(
                        "siteCode", location.getSiteCode() != null ? location.getSiteCode() : "",
                        "locationName", location.getName() != null ? location.getName() : "",
                        "previousValue", oldValue,
                        "newValue", request.isArAvailable())));

        return ApiResponse.ok(null);
    }
}
