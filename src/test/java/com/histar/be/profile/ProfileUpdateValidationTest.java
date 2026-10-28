package com.histar.be.profile;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.histar.be.profile.dto.UpdateProfileRequest;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.controller.ProfileController;
import com.histar.be.profile.service.*;
import com.histar.be.billing.service.BillingService;
import com.histar.be.common.security.CurrentUserAccessor;
import jakarta.validation.Validation;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfileUpdateValidationTest {
    @Test void whitespaceOnlyNameIsInvalidAfterNormalization() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new UpdateProfileRequest("   ", null, null))).isNotEmpty();
            assertThat(factory.getValidator().validate(new UpdateProfileRequest("\u00a0\u00a0", null, null))).isNotEmpty();
        }
    }

    @Test void profileFieldsAreNormalizedBeforeValidation() {
        var request = new UpdateProfileRequest("  Nguyễn Minh  ", " /media/avatar.webp ", "  Huế  ");
        assertThat(request.displayName()).isEqualTo("Nguyễn Minh");
        assertThat(request.avatarUrl()).isEqualTo("/media/avatar.webp");
        assertThat(request.city()).isEqualTo("Huế");
    }

    @Test void avatarRejectsUnsafeSchemesProtocolRelativeAndCredentialUrls() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (String unsafe : new String[]{"javascript:alert(1)", "data:text/html,hello", "//other.example/avatar", "https://user:secret@example.com/a.png", "/\\other.example/a"}) {
                assertThat(validator.validate(new UpdateProfileRequest(null, unsafe, null))).as(unsafe).isNotEmpty();
            }
            for (String safe : new String[]{"https://example.com/a.png", "http://localhost:5173/media/avatar.png", "/media/avatar.webp", ""}) {
                assertThat(validator.validate(new UpdateProfileRequest(null, safe, null))).as(safe).isEmpty();
            }
        }
    }

    @Test void explicitEmptyOptionalFieldsClearSavedValuesWithoutChangingPrivileges() {
        UUID id = UUID.randomUUID();
        var profile = Profile.builder().id(id).displayName("Existing").city("Huế").avatarUrl("/old.png")
                .role("TEACHER").tier("FREE").build();
        var profiles = mock(ProfileService.class);
        var actor = mock(CurrentUserAccessor.class);
        when(actor.getUserId()).thenReturn(Optional.of(id));
        when(profiles.findById(id)).thenReturn(profile);
        when(profiles.save(profile)).thenReturn(profile);
        var controller = new ProfileController(profiles, mock(ProfileMeService.class), mock(BillingService.class),
                actor);
        controller.updateProfile(new UpdateProfileRequest(null, "", ""));
        assertThat(profile.getCity()).isNull();
        assertThat(profile.getAvatarUrl()).isNull();
        assertThat(profile.getDisplayName()).isEqualTo("Existing");
        assertThat(profile.getRole()).isEqualTo("TEACHER");
        assertThat(profile.getTier()).isEqualTo("FREE");
    }
}
