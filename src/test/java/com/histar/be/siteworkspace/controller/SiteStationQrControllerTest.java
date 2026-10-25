package com.histar.be.siteworkspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteStationQrResponse;
import com.histar.be.siteworkspace.service.SiteContentWorkflowService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SiteStationQrControllerTest {

    @Mock
    private SiteContentWorkflowService siteContentWorkflowService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private SiteStationQrController controller;

    @Test
    void staticQrDelegatesAndDoesNotExposeTheSigningSecret() {
        UUID userId = UUID.randomUUID();
        SiteStationQrResponse qr = new SiteStationQrResponse(
                "imperial-city", "ST01", 0L, "sig", "imperial-city:ST01:0:sig", true, true);
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteContentWorkflowService.issueStaticQr(userId, "imperial-city", "ST01")).thenReturn(qr);

        ApiResponse<SiteStationQrResponse> response = controller.issue("imperial-city", "ST01", true);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().payload()).isEqualTo("imperial-city:ST01:0:sig");
        assertThat(response.getData().signature()).isEqualTo("sig");
        assertThat(response.getData().toString()).doesNotContain("qrHmacSecret");
    }

    @Test
    void nonStaticRequestIsRejected() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> controller.issue("imperial-city", "ST01", false))
                .isInstanceOf(BusinessRuleException.class);
        verify(siteContentWorkflowService, never()).issueStaticQr(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unauthenticatedRequestIsRejected() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.issue("imperial-city", "ST01", true))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
    }
}
