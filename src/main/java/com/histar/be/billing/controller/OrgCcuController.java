package com.histar.be.billing.controller;

import com.histar.be.billing.service.CcuSessionService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/org/ccu")
@RequiredArgsConstructor
public class OrgCcuController {

    private final CcuSessionService ccuSessionService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/heartbeat")
    public ApiResponse<Map<String, String>> heartbeat() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        // Keep CCU limit enforcement; unexpected DB errors stay best-effort so they do not spam FE.
        try {
            ccuSessionService.assertCcuAvailable(userId);
        } catch (com.histar.be.common.exception.CcuLimitException ex) {
            throw ex;
        } catch (RuntimeException ignored) {
            // no-op
        }
        return ApiResponse.ok(Map.of("status", "ok"));
    }
}
