package com.histar.be.usercreation.controller;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.usercreation.dto.ShareRecordedResponse;
import com.histar.be.usercreation.dto.UserCreationResponse;
import com.histar.be.usercreation.service.UserCreationAppService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserCreationController {

    private final UserCreationAppService userCreationAppService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping(value = "/user-creations", consumes = "multipart/form-data")
    public ApiResponse<UserCreationResponse> upload(
            @RequestParam UUID frameId,
            @RequestParam(defaultValue = "square") String variant,
            @RequestPart("file") MultipartFile file) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.upload(userId, frameId, variant, file));
    }

    @GetMapping("/me/user-creations")
    public ApiResponse<List<UserCreationResponse>> myCreations() {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.listMine(userId));
    }

    @PostMapping("/user-creations/{id}/record-share")
    public ApiResponse<ShareRecordedResponse> recordShare(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.recordShare(userId, id));
    }
}
