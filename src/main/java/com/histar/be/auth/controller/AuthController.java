package com.histar.be.auth.controller;

import com.histar.be.auth.dto.AuthResponse;
import com.histar.be.auth.dto.LoginRequest;
import com.histar.be.auth.dto.LogoutRequest;
import com.histar.be.auth.dto.RefreshTokenRequest;
import com.histar.be.auth.dto.RegisterRequest;
import com.histar.be.auth.dto.GoogleLoginRequest;
import com.histar.be.auth.dto.VerifyEmailConfirmRequest;
import com.histar.be.auth.dto.VerifyEmailStatusResponse;
import com.histar.be.auth.service.AuthService;
import com.histar.be.auth.service.EmailVerificationService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ApiResponse<AuthResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        return ApiResponse.ok(authService.googleLogin(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok("Logged out", null);
    }

    @PostMapping("/verify-email/resend")
    public ApiResponse<VerifyEmailStatusResponse> resendVerification() {
        UUID userId = currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
        String debugToken = emailVerificationService.sendVerificationEmail(userId);
        return ApiResponse.ok(new VerifyEmailStatusResponse(
                emailVerificationService.isVerified(userId),
                "Đã gửi email xác thực. Vui lòng kiểm tra hộp thư.",
                debugToken));
    }

    @PostMapping("/verify-email/confirm")
    public ApiResponse<VerifyEmailStatusResponse> confirmVerification(
            @RequestBody @Valid VerifyEmailConfirmRequest request) {
        var result = emailVerificationService.confirmToken(request.token());
        return ApiResponse.ok(new VerifyEmailStatusResponse(
                result.verified(), "Email " + result.email() + " đã được xác thực thành công."));
    }
}
