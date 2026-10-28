package com.histar.be.profile.controller;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.common.exception.AuthException;
import com.histar.be.profile.service.AvatarUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
@RestController
@RequestMapping("/api/profile/me/avatar")
@RequiredArgsConstructor
public class ProfileAvatarController {
 private final CurrentUserAccessor actor;
 private final AvatarUploadService avatars;
 @PostMapping(consumes = "multipart/form-data")
 public ApiResponse<Map<String,String>> upload(@RequestParam("file") MultipartFile file) {
  var id=actor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
  return ApiResponse.ok(Map.of("avatarUrl",avatars.upload(id,file)));
 }
}
