package com.histar.be;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

/** Boot 4 splits Flyway auto-configuration out of the generic auto-config module. */
class FlywayBootstrapContractTest {
    @Test void enabledFlywayHasItsRuntimeAutoConfiguration() {
        assertDoesNotThrow(() -> Class.forName("org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"));
    }
}
