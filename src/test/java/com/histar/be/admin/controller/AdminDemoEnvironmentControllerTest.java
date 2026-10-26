package com.histar.be.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.histar.be.admin.dto.DemoEnvironmentStatus;
import com.histar.be.admin.service.DemoEnvironmentService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class AdminDemoEnvironmentControllerTest {

    @Test
    void stopForwardsTheExplicitConfirmationToTheGuardedService() throws Exception {
        DemoEnvironmentService service = mock(DemoEnvironmentService.class);
        Object controller = newController(service);
        Class<?> requestType = Class.forName("com.histar.be.admin.dto.DemoEnvironmentStopRequest");
        Object request = requestType.getConstructor(String.class).newInstance("TẮT DEMO");

        Method stop = controller.getClass().getMethod("stop", requestType);
        stop.invoke(controller, request);

        verify(service).stop("TẮT DEMO");
    }

    @Test
    void statusUsesTheHostBackedService() throws Exception {
        DemoEnvironmentService service = mock(DemoEnvironmentService.class);
        Object controller = newController(service);

        controller.getClass().getMethod("status").invoke(controller);

        verify(service).status();
    }

    private Object newController(DemoEnvironmentService service) throws Exception {
        try {
            Class<?> type = Class.forName("com.histar.be.admin.controller.AdminDemoEnvironmentController");
            Object controller = type.getConstructor(DemoEnvironmentService.class).newInstance(service);
            assertThat(controller).isNotNull();
            return controller;
        } catch (ClassNotFoundException exception) {
            fail("AdminDemoEnvironmentController is required for the admin toggle");
            return null;
        }
    }
}
