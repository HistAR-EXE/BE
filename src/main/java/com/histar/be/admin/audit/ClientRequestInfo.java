package com.histar.be.admin.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves client IP / coarse location from the active HTTP request (if any).
 * No external GeoIP lookup — private and loopback addresses get local labels.
 */
final class ClientRequestInfo {

    private ClientRequestInfo() {}

    record Snapshot(String clientIp, String location) {}

    static Snapshot current() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return new Snapshot(null, null);
        }
        return fromRequest(attrs.getRequest());
    }

    static Snapshot fromRequest(HttpServletRequest request) {
        if (request == null) {
            return new Snapshot(null, null);
        }
        String ip = firstForwardedIp(request.getHeader("X-Forwarded-For"));
        if (ip == null || ip.isBlank()) {
            ip = trimToNull(request.getHeader("X-Real-IP"));
        }
        if (ip == null || ip.isBlank()) {
            ip = trimToNull(request.getRemoteAddr());
        }
        return new Snapshot(ip, resolveLocation(ip));
    }

    static String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String normalized = ip.trim().toLowerCase();
        if ("127.0.0.1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized)
                || "::1".equals(normalized)
                || "localhost".equals(normalized)) {
            return "Localhost";
        }
        if (normalized.startsWith("10.")
                || normalized.startsWith("192.168.")
                || normalized.startsWith("169.254.")
                || isRfc1918_172(normalized)) {
            return "Mạng nội bộ";
        }
        return null;
    }

    private static boolean isRfc1918_172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String firstForwardedIp(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String first = header.split(",")[0];
        return trimToNull(first);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
