package com.histar.be.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.histar.be.admin.audit.AdminAuditEventResponse;
import com.histar.be.admin.audit.AdminAuditExportEnvelope;
import com.histar.be.admin.audit.AdminAuditIntegrityResponse;
import com.histar.be.admin.audit.AdminAuditService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    private final AdminAuditService auditService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ApiResponse<PageResponse<AdminAuditEventResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "desc") String sort) {
        return ApiResponse.ok(auditService.list(
                page, size, entityType, action, actor, actorId, entityId, from, to, sort));
    }

    @GetMapping("/integrity")
    public ApiResponse<AdminAuditIntegrityResponse> integrity(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(
                auditService.verifyIntegrity(entityType, action, actor, actorId, entityId, from, to));
    }

    @GetMapping(value = "/export.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> exportJson(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to)
            throws Exception {
        AdminAuditExportEnvelope envelope =
                auditService.exportJson(entityType, action, actor, actorId, entityId, from, to);
        byte[] body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(envelope);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"histar-audit-export.json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}
