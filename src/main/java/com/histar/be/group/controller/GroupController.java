package com.histar.be.group.controller;

import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.group.dto.CreateGroupRequest;
import com.histar.be.group.dto.GroupDetailResponse;
import com.histar.be.group.dto.GroupProgressResponse;
import com.histar.be.group.dto.GroupSummaryResponse;
import com.histar.be.group.dto.JoinGroupRequest;
import com.histar.be.group.service.GroupService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping
    public ApiResponse<GroupSummaryResponse> create(@RequestBody @Valid CreateGroupRequest request) {
        UUID userId = requireUser();
        return ApiResponse.ok(groupService.createGroup(userId, request));
    }

    @PostMapping("/join")
    public ApiResponse<GroupSummaryResponse> join(@RequestBody @Valid JoinGroupRequest request) {
        UUID userId = requireUser();
        return ApiResponse.ok(groupService.joinGroup(userId, request.code()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<GroupSummaryResponse>> mine() {
        return ApiResponse.ok(groupService.listMine(requireUser()));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupDetailResponse> detail(@PathVariable UUID groupId) {
        return ApiResponse.ok(groupService.getGroup(requireUser(), groupId));
    }

    @GetMapping("/{groupId}/progress")
    public ApiResponse<GroupProgressResponse> progress(@PathVariable UUID groupId) {
        return ApiResponse.ok(groupService.getProgress(requireUser(), groupId));
    }

    @PostMapping("/{groupId}/assign-quest")
    public ApiResponse<GroupSummaryResponse> assignQuest(
            @PathVariable UUID groupId, @RequestBody @Valid com.histar.be.group.dto.AssignGroupQuestRequest request) {
        return ApiResponse.ok(groupService.assignQuest(requireUser(), groupId, request));
    }

    @GetMapping("/multiplayer-access")
    public ApiResponse<Boolean> multiplayerAccess() {
        return ApiResponse.ok(groupService.canUseMultiplayer(requireUser()));
    }

    private UUID requireUser() {
        return currentUserAccessor
                .getUserId()
                .orElseThrow(() -> new com.histar.be.common.exception.AuthException("Unauthorized"));
    }
}
