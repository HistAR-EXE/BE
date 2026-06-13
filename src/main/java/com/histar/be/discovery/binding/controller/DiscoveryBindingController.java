package com.histar.be.discovery.binding.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.discovery.binding.dto.DiscoveryBindingResponse;
import com.histar.be.discovery.binding.repository.DiscoveryContentBindingRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery-bindings")
@RequiredArgsConstructor
public class DiscoveryBindingController {

    private final DiscoveryContentBindingRepository bindingRepository;

    @GetMapping("/by-location/{locationId}")
    public ApiResponse<List<DiscoveryBindingResponse>> byLocation(@PathVariable UUID locationId) {
        List<DiscoveryBindingResponse> items = bindingRepository.findByLocationIdOrderBySortOrder(locationId).stream()
                .map(b -> new DiscoveryBindingResponse(
                        b.getUnlockKey(),
                        b.getRecordKey(),
                        b.getEngagement(),
                        b.getHrefTemplate(),
                        b.getSortOrder() != null ? b.getSortOrder() : 0))
                .toList();
        return ApiResponse.ok(items);
    }
}
