package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.admin.dto.AdminQuestResponse;
import com.histar.be.admin.service.AdminContentService;
import com.histar.be.admin.trash.AdminDeleteImpactResponse;
import com.histar.be.admin.trash.AdminDeleteRequest;
import com.histar.be.admin.trash.AdminRestoreRequest;
import com.histar.be.admin.trash.AdminTrashService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/quests")
@RequiredArgsConstructor
public class AdminQuestController {

    private final AdminContentService adminContentService;
    private final AdminTrashService trashService;
    private final CurrentUserAccessor currentUser;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<?> list(
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "name,asc") String sort) {
        List<AdminQuestResponse> items = adminContentService.listQuests(locationId);
        if (page == null) {
            return ApiResponse.ok(items);
        }
        return ApiResponse.ok(AdminContentPageSupport.page(items, page, size, q, sort, AdminQuestResponse::title));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminQuestResponse> create(@RequestBody @Valid AdminQuestRequest request) {
        return ApiResponse.ok(adminContentService.createQuest(request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminQuestResponse> update(@PathVariable UUID id, @RequestBody @Valid AdminQuestRequest request) {
        return ApiResponse.ok(adminContentService.updateQuest(id, request));
    }

    @GetMapping("/{id}/delete-impact")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDeleteImpactResponse> deleteImpact(@PathVariable UUID id) {
        return ApiResponse.ok(trashService.questImpact(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestBody @Valid AdminDeleteRequest request) {
        trashService.softDeleteQuest(id, actorId(), request);
        return ApiResponse.ok("Quest moved to trash", null);
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> restore(@PathVariable UUID id, @RequestBody @Valid AdminRestoreRequest request) {
        trashService.restoreQuest(id, actorId(), request);
        return ApiResponse.ok("Quest restored", null);
    }

    private UUID actorId() {
        return currentUser.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
    }
}
