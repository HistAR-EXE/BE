package com.histar.be.security;

import com.histar.be.billing.controller.TestOrgQuotaController;
import com.histar.be.config.TestHookProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Central guard for {@code /api/test/**} — required before any test-hook controller runs.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class TestHookSecurityFilter extends OncePerRequestFilter {

    private final TestHookProperties testHookProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path == null || !path.startsWith("/api/test/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!testHookProperties.isEnabled()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String secret = request.getHeader(TestOrgQuotaController.TEST_HOOK_SECRET_HEADER);
        if (testHookProperties.getSecret() == null
                || testHookProperties.getSecret().isBlank()
                || secret == null
                || !testHookProperties.getSecret().equals(secret)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid test hook secret\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
