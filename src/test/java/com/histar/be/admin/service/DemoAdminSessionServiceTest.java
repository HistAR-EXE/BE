package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import com.histar.be.profile.entity.Profile;
import com.histar.be.profile.repository.ProfileRepository;
import com.histar.be.security.JwtService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DemoAdminSessionServiceTest {
    private final DemoProperties properties = new DemoProperties();
    private final ProfileRepository profiles = mock(ProfileRepository.class);
    private final JwtService jwt = mock(JwtService.class);
    private final DemoAdminSessionService service = new DemoAdminSessionService(properties, profiles, jwt);

    DemoAdminSessionServiceTest() {
        properties.setAdminBridgeSecret("a".repeat(64));
        properties.setFrontendUrl("https://demo.timelens.asia");
        properties.setAdminEmail("admin@timelens.test");
    }

    @Test
    void productionTicketExchangesForShortLivedDemoSession() {
        properties.setEnabled(false);
        String ticket = service.issue("admin@timelens.test").url().split("ticket=", 2)[1];
        Profile profile = Profile.builder()
                .id(UUID.randomUUID()).email("admin@timelens.test").displayName("Admin")
                .role("ADMIN").tier("FREE").orgSubscription("NONE")
                .accountStatus("ACTIVE").emailVerified(true).build();
        when(profiles.findByEmail("admin@timelens.test")).thenReturn(Optional.of(profile));
        when(jwt.generateAccessTokenForDuration(
                        eq(profile.getId()), eq(profile.getEmail()), eq("ADMIN"), eq(null), eq("NONE"), anyLong()))
                .thenReturn("demo-access");

        properties.setEnabled(true);
        var response = service.exchange(ticket);
        assertThat(response.accessToken()).isEqualTo("demo-access");
        assertThat(response.refreshToken()).isNull();
        assertThat(response.expiresIn()).isEqualTo(900L);
        assertThatThrownBy(() -> service.exchange(ticket))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void tamperedTicketIsRejected() {
        properties.setEnabled(false);
        String ticket = service.issue("admin@timelens.test").url().split("ticket=", 2)[1] + "x";
        properties.setEnabled(true);
        assertThatThrownBy(() -> service.exchange(ticket))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("invalid");
    }
}
