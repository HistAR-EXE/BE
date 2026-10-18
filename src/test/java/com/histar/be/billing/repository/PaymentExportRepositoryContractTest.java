package com.histar.be.billing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class PaymentExportRepositoryContractTest {

    @Test
    void paymentRepositoriesExposeDatabaseCountAndCappedFetchOperations() {
        assertExportMethods(B2cPaymentTransactionRepository.class);
        assertExportMethods(OrgPaymentTransactionRepository.class);
    }

    private void assertExportMethods(Class<?> repositoryType) {
        assertThat(Arrays.stream(repositoryType.getDeclaredMethods())
                        .filter(method -> method.getName().equals("countForAdminExport")))
                .as("%s countForAdminExport", repositoryType.getSimpleName())
                .hasSize(1);
        assertThat(Arrays.stream(repositoryType.getDeclaredMethods())
                        .filter(method -> method.getName().equals("findForAdminExport")))
                .as("%s findForAdminExport", repositoryType.getSimpleName())
                .singleElement()
                .satisfies(method -> assertThat(method.getParameterTypes()).contains(Pageable.class));
    }
}
