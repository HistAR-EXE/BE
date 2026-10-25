package com.histar.be.siteworkspace.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.AuthException;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.common.response.ApiResponse;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.siteworkspace.dto.SiteContentVersionResponse;
import com.histar.be.siteworkspace.entity.ContentLifecycleStatus;
import com.histar.be.siteworkspace.service.SiteContentWorkflowService;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ExtendWith(MockitoExtension.class)
class SiteContentListControllerTest {

    @Mock
    private SiteContentWorkflowService siteContentWorkflowService;

    @Mock
    private CurrentUserAccessor currentUserAccessor;

    @InjectMocks
    private SiteContentListController controller;

    @Test
    void list_isOptionalStatusGet() throws Exception {
        RequestMapping mapping = SiteContentListController.class.getAnnotation(RequestMapping.class);
        assertThat(mapping.value()).containsExactly("/api/site-workspaces");
        Method method = SiteContentListController.class.getMethod("list", String.class, String.class);
        GetMapping get = method.getAnnotation(GetMapping.class);
        assertThat(get.value()).containsExactly("/{siteCode}/content/versions");
        RequestParam status = method.getParameters()[1].getAnnotation(RequestParam.class);
        assertThat(status.name()).isEqualTo("status");
        assertThat(status.required()).isFalse();
    }

    @Test
    void list_wrapsFilteredVersions() {
        UUID userId = UUID.randomUUID();
        List<SiteContentVersionResponse> rows = List.of();
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteContentWorkflowService.listVersions(userId, "imperial-city", ContentLifecycleStatus.IN_REVIEW))
                .thenReturn(rows);

        ApiResponse<List<SiteContentVersionResponse>> response = controller.list("imperial-city", "in_review");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(rows);
    }

    @Test
    void list_blankStatusIsUnfiltered() {
        UUID userId = UUID.randomUUID();
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(userId));
        when(siteContentWorkflowService.listVersions(eq(userId), eq("imperial-city"), isNull()))
                .thenReturn(List.of());

        ApiResponse<List<SiteContentVersionResponse>> response = controller.list("imperial-city", " ");

        assertThat(response.getData()).isEmpty();
        verify(siteContentWorkflowService).listVersions(eq(userId), eq("imperial-city"), isNull());
    }

    @Test
    void list_rejectsUnknownStatus() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));

        assertThatThrownBy(() -> controller.list("imperial-city", "NOPE"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("status không hợp lệ");
    }

    @Test
    void list_throwsWhenUnauthenticated() {
        when(currentUserAccessor.getUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.list("imperial-city", null))
                .isInstanceOf(AuthException.class)
                .hasMessage("Unauthorized");
    }
}
