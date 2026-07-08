package com.histar.be.auth.controller;

import com.histar.be.auth.service.EmailVerificationService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.ResourceNotFoundException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.config.TestHookProperties;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test/auth")
@RequiredArgsConstructor
public class TestAuthController {

    public static final String TEST_HOOK_SECRET_HEADER = "X-Test-Hook-Secret";

    private final TestHookProperties testHookProperties;
    private final EmailVerificationService emailVerificationService;

    /** E2E/API only: mark email verified without SMTP inbox (when MAIL_ENABLED=true). */
    @PostMapping("/{userId}/verify-email")
    public ApiResponse<Void> forceVerifyEmail(
            @PathVariable UUID userId,
            @RequestHeader(value = TEST_HOOK_SECRET_HEADER, required = false) String secret) {
        assertTestHook(secret);
        emailVerificationService.markVerifiedFromOAuth(userId);
        return ApiResponse.ok(null);
    }

    private void assertTestHook(String secret) {
        if (!testHookProperties.isEnabled()) {
            throw new ResourceNotFoundException("Not found");
        }
        if (testHookProperties.getSecret() == null
                || testHookProperties.getSecret().isBlank()
                || secret == null
                || !testHookProperties.getSecret().equals(secret)) {
            throw new AuthException("Invalid test hook secret");
        }
    }
}
