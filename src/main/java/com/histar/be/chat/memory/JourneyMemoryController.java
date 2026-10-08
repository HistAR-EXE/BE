package com.histar.be.chat.memory;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/memory")
@RequiredArgsConstructor
public class JourneyMemoryController {

    private final JourneyMemoryService journeyMemoryService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<JourneyMemoryResponse> get(@RequestParam UUID locationId) {
        UUID userId = userId();
        JourneyMemory memory = journeyMemoryService.findActive(userId, locationId)
                .orElseGet(() -> JourneyMemory.disabled(userId, locationId));
        return ApiResponse.ok(JourneyMemoryResponse.from(memory));
    }

    @PatchMapping
    public ApiResponse<JourneyMemoryResponse> setEnabled(
            @RequestParam UUID locationId, @RequestBody JourneyMemorySetting request) {
        return ApiResponse.ok(JourneyMemoryResponse.from(
                journeyMemoryService.setEnabled(userId(), locationId, request.enabled())));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(@RequestParam UUID locationId) {
        journeyMemoryService.deleteForLocation(userId(), locationId);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAll() {
        journeyMemoryService.deleteAll(userId());
        return ApiResponse.ok(null);
    }

    private UUID userId() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }

    public record JourneyMemorySetting(boolean enabled) {}
}
