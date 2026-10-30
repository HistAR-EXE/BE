package com.histar.be.chat.controller;

import com.histar.be.chat.service.ChatVoiceService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.service.TierAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Keeps the AI network private; browser audio goes through the authenticated backend. */
@RestController
@RequestMapping("/api/chat/voice")
@RequiredArgsConstructor
public class ChatVoiceController {
    private final CurrentUserAccessor currentUserAccessor;
    private final ChatVoiceService voiceService;
    private final TierAccessService tierAccessService;

    public record SpeechRequest(@NotBlank @Size(max = 1200) String text,
                                @Size(max = 64) String persona_key) {}

    @PostMapping(value = "/tts")
    public ResponseEntity<byte[]> synthesize(@RequestBody @Valid SpeechRequest request) {
        var userId = currentUserAccessor.getUserId().orElseThrow(() -> new AuthException("Unauthorized"));
        if (!tierAccessService.hasPremiumAccess(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN, "Gọi video cùng Chrono chỉ dành cho gói Premium.");
        }
        var audio = voiceService.synthesize(request.text(), request.persona_key());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(audio.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(audio.bytes());
    }
}
