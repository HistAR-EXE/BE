package com.histar.be.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.device.dto.RegisterPushTokenRequest;
import com.histar.be.device.entity.DevicePushToken;
import com.histar.be.device.repository.DevicePushTokenRepository;
import com.histar.be.device.service.DevicePushTokenService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DevicePushTokenServiceTest {

    @Mock
    private DevicePushTokenRepository repository;

    @InjectMocks
    private DevicePushTokenService service;

    @Test
    void upsertInsertsNewRow() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdAndPlatform(userId, "ANDROID")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.upsert(userId, new RegisterPushTokenRequest("fcm-token-1", "ANDROID"));

        ArgumentCaptor<DevicePushToken> cap = ArgumentCaptor.forClass(DevicePushToken.class);
        verify(repository).save(cap.capture());
        assertThat(cap.getValue().getToken()).isEqualTo("fcm-token-1");
        assertThat(cap.getValue().getUserId()).isEqualTo(userId);
    }
}
