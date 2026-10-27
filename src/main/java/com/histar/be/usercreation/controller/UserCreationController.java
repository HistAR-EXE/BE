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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserCreationController {

    private final UserCreationAppService userCreationAppService;
    private final CurrentUserAccessor currentUserAccessor;

    @PostMapping(value = "/user-creations", consumes = "multipart/form-data")
    public ApiResponse<UserCreationResponse> upload(
            @RequestParam(required = false) UUID frameId,
            @RequestParam(defaultValue = "square") String variant,
            @RequestParam(defaultValue = "original") String kind,
            @RequestParam(required = false) String siteCode,
            @RequestParam(required = false) String stationCode,
            @RequestParam(required = false) String roleId,
            @RequestParam(required = false) Integer era,
            @RequestParam(required = false) String source,
            @RequestPart("file") MultipartFile file) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.upload(userId, frameId, variant, kind, siteCode, stationCode, roleId, era, source, file));
    }

    @GetMapping("/me/user-creations")
    public ApiResponse<List<UserCreationResponse>> myCreations() {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.listMine(userId));
    }

    @DeleteMapping("/user-creations/{id}")
    public ResponseEntity<Void> deleteMine(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        userCreationAppService.deleteMine(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user-creations/{id}/image")
    public ResponseEntity<byte[]> downloadMine(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.noStore())
                .body(userCreationAppService.downloadMine(userId, id));
    }

    @PostMapping("/user-creations/{id}/record-share")
    public ApiResponse<ShareRecordedResponse> recordShare(@PathVariable UUID id) {
        UUID userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        return ApiResponse.ok(userCreationAppService.recordShare(userId, id));
    }
}
