package com.histar.be.admin.controller;

import com.histar.be.admin.trash.AdminPurgeRequest;
import com.histar.be.admin.trash.AdminTrashBulkRequest;
import com.histar.be.admin.trash.AdminTrashBulkResult;
import com.histar.be.admin.trash.AdminTrashEmptyRequest;
import com.histar.be.admin.trash.AdminTrashEmptyResult;
import com.histar.be.admin.trash.AdminTrashItem;
import com.histar.be.admin.trash.AdminTrashService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trash")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrashController {

    private final AdminTrashService trashService;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    public ApiResponse<PageResponse<AdminTrashItem>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "deletedAt,desc") String sort) {
        return ApiResponse.ok(trashService.list(page, size, type, locationId, search, sort));
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResponse<Void> purge(
            @PathVariable String type, @PathVariable UUID id, @RequestBody @Valid AdminPurgeRequest request) {
        trashService.purge(type, id, actorId(), request);
        return ApiResponse.ok("Purged permanently", null);
    }

    @PostMapping("/bulk")
    public ApiResponse<AdminTrashBulkResult> bulk(@RequestBody @Valid AdminTrashBulkRequest request) {
        return ApiResponse.ok(trashService.bulk(actorId(), request));
    }

    @PostMapping("/empty")
    public ApiResponse<AdminTrashEmptyResult> empty(@RequestBody @Valid AdminTrashEmptyRequest request) {
        return ApiResponse.ok(trashService.empty(actorId(), request));
    }

    private UUID actorId() {
        return currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
