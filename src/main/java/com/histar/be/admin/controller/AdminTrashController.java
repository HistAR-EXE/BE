package com.histar.be.admin.controller;

import com.histar.be.admin.trash.AdminTrashItem;
import com.histar.be.admin.trash.AdminTrashService;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.response.PageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trash")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTrashController {

    private final AdminTrashService trashService;

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
}
