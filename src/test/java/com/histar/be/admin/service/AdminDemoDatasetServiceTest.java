package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.histar.be.common.security.CurrentUserAccessor;
import com.histar.be.config.DemoProperties;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AdminDemoDatasetServiceTest {
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final CurrentUserAccessor currentUser = mock(CurrentUserAccessor.class);
    private final DemoProperties properties = new DemoProperties();

    @Test
    void productionEnvironmentCannotPurgeDemoAccounts() {
        properties.setEnabled(false);
        var service = new AdminDemoDatasetService(jdbc, currentUser, properties);
        assertThatThrownBy(() -> service.purge(null, "Dọn dữ liệu demo sau buổi trình diễn"))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class);
        verifyNoInteractions(jdbc);
    }

    @Test
    void purgeRequiresAnAuthenticatedAdminActorAndReason() {
        properties.setEnabled(true);
        var service = new AdminDemoDatasetService(jdbc, currentUser, properties);
        when(currentUser.getUserId()).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.purge(null, "Dọn dữ liệu demo sau buổi trình diễn"))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class);
        when(currentUser.getUserId()).thenReturn(Optional.of(UUID.randomUUID()));
        assertThatThrownBy(() -> service.purge(null, " "))
                .isInstanceOf(com.histar.be.common.exception.BusinessRuleException.class);
        verifyNoInteractions(jdbc);
    }
}
