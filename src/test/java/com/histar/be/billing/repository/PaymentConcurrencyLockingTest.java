package com.histar.be.billing.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

class PaymentConcurrencyLockingTest {

    @Test
    void b2cMutationLookupsUsePessimisticWriteLocks() {
        assertPessimisticWriteMethods(
                B2cPaymentTransactionRepository.class,
                "findByIdForUpdate",
                "findByOrderCodeForUpdate",
                "findByTransferContentForUpdate");
    }

    @Test
    void b2bMutationLookupsUsePessimisticWriteLocks() {
        assertPessimisticWriteMethods(
                OrgPaymentTransactionRepository.class,
                "findByIdForUpdate",
                "findByOrderCodeForUpdate",
                "findByTransferContentForUpdate");
    }

    private void assertPessimisticWriteMethods(Class<?> repositoryType, String... methodNames) {
        for (String methodName : methodNames) {
            Method method = Arrays.stream(repositoryType.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(methodName))
                    .findFirst()
                    .orElse(null);
            assertThat(method)
                    .as("%s must declare %s", repositoryType.getSimpleName(), methodName)
                    .isNotNull();
            assertThat(method.getAnnotation(Lock.class))
                    .as("%s.%s lock annotation", repositoryType.getSimpleName(), methodName)
                    .isNotNull()
                    .extracting(Lock::value)
                    .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        }
    }
}
