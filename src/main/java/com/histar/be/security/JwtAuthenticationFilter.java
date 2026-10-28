package com.histar.be.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final com.histar.be.billing.service.CcuSessionService ccuSessions;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token)) {
                String email = jwtService.extractEmail(token);
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    if (userDetails.isEnabled()) {
                        var auth = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        String path = request.getRequestURI();
                        boolean identityOrExit = path.startsWith("/api/auth/") || path.equals("/api/profile/me")
                                || path.equals("/api/profile/me/avatar")
                                || path.equals("/api/profile/overview")
                                || path.equals("/api/org/mine") || path.equals("/api/org/leave")
                                || path.matches("/api/org/[^/]+/capabilities");
                        boolean admin = userDetails.getAuthorities().stream()
                                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
                        if (path.startsWith("/api/") && !identityOrExit && !admin) {
                            try {
                                ccuSessions.admitRequest(email);
                            } catch (com.histar.be.common.exception.CcuLimitException full) {
                                rejectAdmission(response, "CCU_LIMIT_EXCEEDED", full.getMessage());
                                return;
                            } catch (com.histar.be.common.exception.BusinessRuleException inactive) {
                                rejectAdmission(response, "ORG_ACCESS_INACTIVE", inactive.getMessage());
                                return;
                            }
                        }
                    }
                } catch (UsernameNotFoundException ignored) {
                    // A valid JWT may outlive an account removed by an admin.
                    // Continue unauthenticated so Security returns 401/403, not 500.
                    SecurityContextHolder.clearContext();
                }
            }
        }
        chain.doFilter(request, response);
    }

    private void rejectAdmission(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(403);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), java.util.Map.of(
                "success", false, "code", code, "message", message, "type", "ORG_CCU"));
    }
}
