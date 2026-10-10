package com.histar.be.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Keeps scrape endpoints separate from user JWT authorization. */
@Component
public class MonitoringBearerTokenFilter extends OncePerRequestFilter {
    private final String token;

    public MonitoringBearerTokenFilter(@Value("${histar.monitoring.bearer-token:}") String token) {
        this.token = token == null ? "" : token;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String provided = request.getHeader("Authorization");
        String expected = "Bearer " + token;
        if (token.isBlank() || provided == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Monitoring bearer token required");
            return;
        }
        chain.doFilter(request, response);
    }
}
