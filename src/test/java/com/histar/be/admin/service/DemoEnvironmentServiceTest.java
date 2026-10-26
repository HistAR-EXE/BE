package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.histar.be.admin.dto.DemoEnvironmentStatus;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class DemoEnvironmentServiceTest {

    @Test
    void stopRefusesWhenTheLatestHostCheckFindsActiveUsers() throws Exception {
        DemoEnvironmentHostClient host = mock(DemoEnvironmentHostClient.class);
        when(host.status()).thenReturn(new DemoEnvironmentStatus(true, 2));
        Object service = newService(host);

        assertThatThrownBy(() -> invoke(service, "stop", "TẮT DEMO"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("2 người đang hoạt động");
        verify(host, never()).stop();
    }

    @Test
    void stopRequiresTheExplicitVietnameseConfirmationPhrase() throws Exception {
        DemoEnvironmentHostClient host = mock(DemoEnvironmentHostClient.class);
        Object service = newService(host);

        assertThatThrownBy(() -> invoke(service, "stop", "tat demo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("TẮT DEMO");
        verify(host, never()).stop();
    }

    private Object newService(DemoEnvironmentHostClient host) throws Exception {
        DemoProperties properties = new DemoProperties();
        properties.setEnabled(false);
        try {
            Class<?> type = Class.forName("com.histar.be.admin.service.DemoEnvironmentService");
            return type.getConstructor(DemoProperties.class, DemoEnvironmentHostClient.class)
                    .newInstance(properties, host);
        } catch (ClassNotFoundException exception) {
            fail("DemoEnvironmentService is required for the admin toggle");
            return null;
        }
    }

    private Object invoke(Object target, String method, String argument) throws Throwable {
        try {
            Method selected = target.getClass().getMethod(method, String.class);
            return selected.invoke(target, argument);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }
}
