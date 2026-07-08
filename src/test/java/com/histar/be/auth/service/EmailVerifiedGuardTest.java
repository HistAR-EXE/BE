package com.histar.be.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.EmailNotVerifiedException;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerifiedGuardTest {

    @Mock
    private ProfileRepository profileRepository;

    @InjectMocks
    private EmailVerifiedGuard emailVerifiedGuard;

    @Test
    void assertEmailVerified_throwsWhenNotVerified() {
        UUID userId = UUID.randomUUID();
        when(profileRepository.findById(userId))
                .thenReturn(Optional.of(Profile.builder().id(userId).emailVerified(false).build()));

        assertThatThrownBy(() -> emailVerifiedGuard.assertEmailVerified(userId))
                .isInstanceOf(EmailNotVerifiedException.class);
    }
}
