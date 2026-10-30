package com.histar.be.chat.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.chat.service.ChatVoiceService;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.ErrorCode;
import com.histar.be.profile.service.TierAccessService;
import com.histar.be.common.security.CurrentUserAccessor;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatVoiceControllerTest {
    private final TierAccessService tiers = mock(TierAccessService.class);
    @Test void requiresAuthenticationBeforeCallingAi() {
        var user = mock(CurrentUserAccessor.class);
        var service = mock(ChatVoiceService.class);
        when(user.getUserId()).thenReturn(Optional.empty());
        var controller = new ChatVoiceController(user, service, tiers);
        assertThatThrownBy(() -> controller.synthesize(new ChatVoiceController.SpeechRequest("Xin chào", "chrono")))
                .isInstanceOf(AuthException.class);
        verifyNoInteractions(service);
    }
    @Test void freeAccountCannotGenerateSpeechEvenWithADirectApiCall() {
        var user = mock(CurrentUserAccessor.class);
        var service = mock(ChatVoiceService.class);
        UUID id = UUID.randomUUID();
        when(user.getUserId()).thenReturn(Optional.of(id));
        when(tiers.hasPremiumAccess(id)).thenReturn(false);
        var controller = new ChatVoiceController(user, service, tiers);
        assertThatThrownBy(() -> controller.synthesize(new ChatVoiceController.SpeechRequest("Xin chào", "chrono")))
                .isInstanceOf(BaseException.class)
                .satisfies(error -> assertThat(((BaseException)error).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
        verifyNoInteractions(service);
    }
    @Test void returnsPrivateAudioWithoutCaching() {
        when(tiers.hasPremiumAccess(any(UUID.class))).thenReturn(true);
        var user = mock(CurrentUserAccessor.class);
        var service = mock(ChatVoiceService.class);
        when(user.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        when(service.synthesize("Xin chào", "chrono")).thenReturn(new ChatVoiceService.SpeechAudio(new byte[]{1, 2}, "audio/wav"));
        var response = new ChatVoiceController(user, service, tiers)
                .synthesize(new ChatVoiceController.SpeechRequest("Xin chào", "chrono"));
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("audio/wav");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).containsExactly((byte)1, (byte)2);
    }
}
