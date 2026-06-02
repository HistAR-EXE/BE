package com.histar.be.security;

import com.histar.be.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityExceptionHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Unauthorized");
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        writeError(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN, "Forbidden");
    }

    private void writeError(HttpServletResponse response, int status, ErrorCode code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String payload = "{"
                + "\"success\":false,"
                + "\"code\":\"" + safe(code.getCode()) + "\","
                + "\"message\":\"" + safe(message) + "\","
                + "\"timestamp\":\"" + java.time.Instant.now() + "\""
                + "}";
        response.getWriter().write(payload);
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
