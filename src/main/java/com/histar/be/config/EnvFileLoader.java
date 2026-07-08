package com.histar.be.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads {@code .env} (and {@code .env.production} when profile is prod) from the working
 * directory into system properties before Spring starts. Does not override variables already
 * set in the OS environment.
 */
public final class EnvFileLoader {

    private static final Logger log = LoggerFactory.getLogger(EnvFileLoader.class);

    private EnvFileLoader() {}

    public static void loadIfPresent() {
        Path baseDir = Path.of(System.getProperty("user.dir"));
        List<Path> candidates = new ArrayList<>();
        candidates.add(baseDir.resolve(".env"));
        if (isProdProfile()) {
            candidates.add(baseDir.resolve(".env.production"));
        }
        int totalLoaded = 0;
        for (int i = 0; i < candidates.size(); i++) {
            totalLoaded += loadFile(candidates.get(i), i > 0);
        }
        if (totalLoaded > 0) {
            log.info("Loaded {} env entries from {}", totalLoaded, candidates);
        }
    }

    private static boolean isProdProfile() {
        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile == null || profile.isBlank()) {
            profile = System.getProperty("SPRING_PROFILES_ACTIVE");
        }
        return profile != null && profile.contains("prod");
    }

    private static int loadFile(Path envFile, boolean overridePreviousFile) {
        if (!Files.isRegularFile(envFile)) {
            log.debug("Env file not found at {}", envFile.toAbsolutePath());
            return 0;
        }
        try {
            List<String> lines = Files.readAllLines(envFile, StandardCharsets.UTF_8);
            int loaded = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = stripQuotes(trimmed.substring(eq + 1).trim());
                if (key.isEmpty()) {
                    continue;
                }
                if (System.getenv(key) != null) {
                    continue;
                }
                if (!overridePreviousFile && System.getProperty(key) != null) {
                    continue;
                }
                System.setProperty(key, value);
                loaded++;
            }
            log.info("Loaded {} entries from {}", loaded, envFile.toAbsolutePath());
            return loaded;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load env from " + envFile.toAbsolutePath(), ex);
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
