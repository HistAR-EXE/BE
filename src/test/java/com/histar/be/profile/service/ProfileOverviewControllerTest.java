package com.histar.be.profile.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.common.exception.AuthException;
import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.profile.controller.ProfileOverviewController;
import com.histar.be.profile.dto.ProfileOverviewResponse;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileOverviewControllerTest {
    @Test void anonymousRequestDoesNotReadAnyProfile() {
        var accessor = mock(CurrentUserAccessor.class);
        var overview = mock(ProfileOverviewService.class);
        when(accessor.getUserId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new ProfileOverviewController(accessor, overview).me()).isInstanceOf(AuthException.class);
        verifyNoInteractions(overview);
    }

    @Test void profileIdentifierComesOnlyFromAuthenticatedSession() {
        UUID userId = UUID.randomUUID();
        var accessor = mock(CurrentUserAccessor.class);
        var overview = mock(ProfileOverviewService.class);
        var response = mock(ProfileOverviewResponse.class);
        when(accessor.getUserId()).thenReturn(Optional.of(userId));
        when(overview.overview(userId)).thenReturn(response);
        assertThat(new ProfileOverviewController(accessor, overview).me().getData()).isSameAs(response);
        verify(overview).overview(userId);
        verifyNoMoreInteractions(overview);
    }
}
