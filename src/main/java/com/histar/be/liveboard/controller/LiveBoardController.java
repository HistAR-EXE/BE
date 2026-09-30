package com.histar.be.liveboard.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import com.histar.be.liveboard.dto.RallyResponse;
import com.histar.be.liveboard.service.LiveBoardService;
import com.histar.be.organization.service.OrgAccessService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Teacher/admin-only live board (all routes enforce {@link OrgAccessService#requireOrgAccess}). */
@RestController
@RequestMapping("/api/org/{orgId}/live-board")
@RequiredArgsConstructor
public class LiveBoardController {

    private final LiveBoardService liveBoardService;
    private final OrgAccessService orgAccessService;
    private final CurrentUserAccessor currentUserAccessor;

    @GetMapping
    public ApiResponse<LiveBoardSnapshot> snapshot(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        return ApiResponse.ok(liveBoardService.snapshot(orgId));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .header("X-Accel-Buffering", "no")
                .body(liveBoardService.subscribe(orgId));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        byte[] body = liveBoardService.exportCsv(orgId).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"histar-live-board.csv\"")
                .body(body);
    }

    @PostMapping("/rally")
    public ApiResponse<RallyResponse> rally(@PathVariable UUID orgId) {
        orgAccessService.requireOrgAccess(orgId);
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(liveBoardService.rally(orgId, userId));
    }
}
