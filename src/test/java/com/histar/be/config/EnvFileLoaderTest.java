package com.histar.be.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class EnvFileLoaderTest {

    private static final String TEST_PROPERTY = "HISTAR_ENV_FILE_LOADER_TEST_VALUE";

    @Test
    void doesNotLoadDotEnvWhenExplicitlyDisabled() throws Exception {
        String originalDirectory = System.getProperty("user.dir");
        String originalEnabled = System.getProperty("histar.dotenv.enabled");
        String originalValue = System.getProperty(TEST_PROPERTY);
        Path temporaryDirectory = Files.createTempDirectory("histar-dotenv-disabled-");
        Files.writeString(temporaryDirectory.resolve(".env"), TEST_PROPERTY + "=loaded-from-dotenv\n");

        try {
            System.setProperty("user.dir", temporaryDirectory.toString());
            System.setProperty("histar.dotenv.enabled", "false");
            System.clearProperty(TEST_PROPERTY);

            EnvFileLoader.loadIfPresent();

            assertThat(System.getProperty(TEST_PROPERTY)).isNull();
        } finally {
            restore("user.dir", originalDirectory);
            restore("histar.dotenv.enabled", originalEnabled);
            restore(TEST_PROPERTY, originalValue);
            Files.deleteIfExists(temporaryDirectory.resolve(".env"));
            Files.deleteIfExists(temporaryDirectory);
        }
    }

    private static void restore(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
