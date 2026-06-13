package com.histar.be.admin.controller;

import com.histar.be.admin.dto.AdminQuestRequest;
import com.histar.be.admin.dto.AdminQuestResponse;
import com.histar.be.admin.service.AdminContentService;
import com.histar.be.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AdminQuestResponse>> list(@RequestParam(required = false) UUID locationId) {
        return ApiResponse.ok(adminContentService.listQuests(locationId));
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
}
