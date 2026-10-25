package com.histar.be.siteworkspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.CreateSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.ReviewSiteOnboardingRequest;
import com.histar.be.siteworkspace.dto.SiteOnboardingResponse;
import com.histar.be.siteworkspace.entity.SiteOnboardingStatus;
import com.histar.be.siteworkspace.service.SiteOnboardingService;
import jakarta.validation.Valid;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ExtendWith(MockitoExtension.class)
class SiteOnboardingControllerTest {

    @Mock
    private SiteOnboardingService siteOnboardingService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private SiteOnboardingController siteOnboardingController;

    @InjectMocks
    private AdminSiteOnboardingController adminSiteOnboardingController;

    @Test
    void submit_usesAuthenticatedCaller() {
        UUID actorId = UUID.randomUUID();
        CreateSiteOnboardingRequest request = request();
        SiteOnboardingResponse body = response(actorId);
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(siteOnboardingService.submit(actorId, request)).thenReturn(body);

        ApiResponse<SiteOnboardingResponse> response = siteOnboardingController.submit(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(body);
        verify(siteOnboardingService).submit(actorId, request);
    }

    @Test
    void submit_throwsWhenUnauthenticated() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> siteOnboardingController.submit(request()))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
        verify(siteOnboardingService, never()).submit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void mine_usesAuthenticatedCaller() {
        UUID actorId = UUID.randomUUID();
        List<SiteOnboardingResponse> mine = List.of(response(actorId));
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(actorId));
        when(siteOnboardingService.listMine(actorId)).thenReturn(mine);

        ApiResponse<List<SiteOnboardingResponse>> response = siteOnboardingController.mine();

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo(mine);
    }

    @Test
    void adminReview_usesAuthenticatedAdminAndDoesNotTakeUserIdFromBody() {
        UUID adminId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        SiteOnboardingResponse approved = response(UUID.randomUUID());
        ReviewSiteOnboardingRequest review = new ReviewSiteOnboardingRequest("Hồ sơ hợp lệ");
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(adminId));
        when(siteOnboardingService.approve(requestId, adminId, "Hồ sơ hợp lệ")).thenReturn(approved);
        when(siteOnboardingService.reject(requestId, adminId, "Thiếu giấy ủy quyền")).thenReturn(approved);
        when(siteOnboardingService.listByStatus(SiteOnboardingStatus.PENDING)).thenReturn(List.of(approved));

        assertThat(adminSiteOnboardingController.list(SiteOnboardingStatus.PENDING).getData()).containsExactly(approved);
        assertThat(adminSiteOnboardingController.approve(requestId, review).getData()).isEqualTo(approved);
        assertThat(adminSiteOnboardingController.reject(requestId, new ReviewSiteOnboardingRequest("Thiếu giấy ủy quyền"))
                        .isSuccess())
                .isTrue();
        verify(siteOnboardingService).approve(requestId, adminId, "Hồ sơ hợp lệ");
        verify(siteOnboardingService).reject(requestId, adminId, "Thiếu giấy ủy quyền");
    }

    @Test
    void adminReview_throwsWhenUnauthenticated() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());
        UUID requestId = UUID.randomUUID();

        assertThatThrownBy(() -> adminSiteOnboardingController.approve(requestId, new ReviewSiteOnboardingRequest("ok")))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
        assertThatThrownBy(() -> adminSiteOnboardingController.reject(requestId, new ReviewSiteOnboardingRequest("no")))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
    }

    @Test
    void routesRequireAdminOnlyOnReviewEndpoints() throws Exception {
        assertThat(SiteOnboardingController.class.getAnnotation(PreAuthorize.class)).isNull();
        assertThat(SiteOnboardingController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/site-onboarding-requests");
        assertThat(SiteOnboardingController.class.getMethod("submit", CreateSiteOnboardingRequest.class)
                        .getAnnotation(PostMapping.class)
                        .value())
                .isEmpty();
        assertThat(SiteOnboardingController.class.getMethod("mine").getAnnotation(GetMapping.class).value())
                .containsExactly("/mine");

        PreAuthorize admin = AdminSiteOnboardingController.class.getAnnotation(PreAuthorize.class);
        assertThat(admin.value()).isEqualTo("hasRole('ADMIN')");
        assertThat(AdminSiteOnboardingController.class.getAnnotation(RequestMapping.class).value())
                .containsExactly("/api/admin/site-onboarding-requests");

        Method list = AdminSiteOnboardingController.class.getMethod("list", SiteOnboardingStatus.class);
        RequestParam status = list.getParameters()[0].getAnnotation(RequestParam.class);
        assertThat(status.name()).isEqualTo("status");
        assertThat(status.defaultValue()).isEqualTo("PENDING");
        assertThat(AdminSiteOnboardingController.class
                        .getMethod("approve", UUID.class, ReviewSiteOnboardingRequest.class)
                        .getAnnotation(PostMapping.class)
                        .value())
                .containsExactly("/{id}/approve");
        assertThat(AdminSiteOnboardingController.class
                        .getMethod("reject", UUID.class, ReviewSiteOnboardingRequest.class)
                        .getAnnotation(PostMapping.class)
                        .value())
                .containsExactly("/{id}/reject");
        assertThat(reviewBody("approve").getAnnotation(Valid.class)).isNotNull();
        assertThat(reviewBody("reject").getAnnotation(Valid.class)).isNotNull();
    }

    private static Parameter reviewBody(String methodName) throws Exception {
        return AdminSiteOnboardingController.class
                .getMethod(methodName, UUID.class, ReviewSiteOnboardingRequest.class)
                .getParameters()[1];
    }

    private CreateSiteOnboardingRequest request() {
        return new CreateSiteOnboardingRequest(
                "Nguyễn Văn A",
                "Trung tâm Bảo tồn Di tích",
                "van.a@ditich.vn",
                "imperial-city",
                "Giấy ủy quyền số 12");
    }

    private SiteOnboardingResponse response(UUID requesterId) {
        Instant createdAt = Instant.parse("2026-10-24T01:00:00Z");
        return new SiteOnboardingResponse(
                UUID.randomUUID(),
                requesterId,
                "imperial-city",
                "Nguyễn Văn A",
                "Trung tâm Bảo tồn Di tích",
                "van.a@ditich.vn",
                "Giấy ủy quyền số 12",
                SiteOnboardingStatus.PENDING,
                null,
                null,
                createdAt,
                null);
    }
}
