package com.histar.be.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import com.histar.be.admin.service.DemoActivityService;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class DemoActivityControllerTest {

    @Test
    void heartbeatRegistersTheAuthenticatedDemoUser() throws Exception {
        Fixture fixture = fixture();
        Method heartbeat = fixture.controller.getClass().getMethod("heartbeat", org.springframework.security.core.Authentication.class);

        heartbeat.invoke(fixture.controller, new UsernamePasswordAuthenticationToken("user@demo.test", null));
        Object snapshot = fixture.controller.getClass().getMethod("internalStatus", String.class)
                .invoke(fixture.controller, "Bearer " + "c".repeat(64));

        Object data = snapshot.getClass().getMethod("getData").invoke(snapshot);
        assertThat(data.getClass().getMethod("activeUsers").invoke(data)).isEqualTo(1);
    }

    @Test
    void internalStatusRejectsAnInvalidHostControllerSecret() throws Exception {
        Fixture fixture = fixture();
        Method status = fixture.controller.getClass().getMethod("internalStatus", String.class);

        assertThatThrownBy(() -> invoke(status, fixture.controller, "Bearer wrong"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không hợp lệ");
    }

    private Fixture fixture() throws Exception {
        DemoProperties properties = new DemoProperties();
        properties.setEnabled(true);
        try {
            DemoProperties.class.getMethod("setControlSecret", String.class)
                    .invoke(properties, "c".repeat(64));
        } catch (NoSuchMethodException exception) {
            fail("Demo control secret configuration is required");
        }
        DemoActivityService service = new DemoActivityService();
        try {
            Class<?> type = Class.forName("com.histar.be.admin.controller.DemoActivityController");
            Object controller = type.getConstructor(DemoProperties.class, DemoActivityService.class)
                    .newInstance(properties, service);
            return new Fixture(controller);
        } catch (ClassNotFoundException exception) {
            fail("DemoActivityController is required for activity heartbeat and safe shutdown");
            return null;
        }
    }

    private Object invoke(Method method, Object target, Object argument) throws Throwable {
        try {
            return method.invoke(target, argument);
        } catch (InvocationTargetException exception) {
            throw exception.getCause();
        }
    }

    private record Fixture(Object controller) {}
}
