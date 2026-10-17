package com.histar.be.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class AdminPaymentControllerContractTest {

    @Test
    void allPaymentOperationsAndCsvExportRequireAdmin() {
        PreAuthorize authorization = AdminB2cPaymentController.class.getAnnotation(PreAuthorize.class);
        assertThat(authorization).isNotNull();
        assertThat(authorization.value()).isEqualTo("hasRole('ADMIN')");

        assertMapping("markB2cPaid", PostMapping.class, "/b2c/{id}/mark-paid");
        assertMapping("refundB2c", PostMapping.class, "/b2c/{id}/refund");
        assertMapping("voidB2c", PostMapping.class, "/b2c/{id}/void");
        assertMapping("markB2bPaid", PostMapping.class, "/b2b/{id}/mark-paid");
        assertMapping("refundB2b", PostMapping.class, "/b2b/{id}/refund");
        assertMapping("voidB2b", PostMapping.class, "/b2b/{id}/void");
        assertMapping("exportCsv", GetMapping.class, "/export.csv");
    }

    private void assertMapping(String methodName, Class<?> annotationType, String path) {
        Method method = Arrays.stream(AdminB2cPaymentController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
        if (annotationType == PostMapping.class) {
            assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(path);
        } else {
            assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
        }
    }
}
