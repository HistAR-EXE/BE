package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private RagAi ragAi = new RagAi();
    private Resilience resilience = new Resilience();

    public RagAi getRagAi() {
        return ragAi;
    }

    public void setRagAi(RagAi ragAi) {
        this.ragAi = ragAi;
    }

    public Resilience getResilience() {
        return resilience;
    }

    public void setResilience(Resilience resilience) {
        this.resilience = resilience;
    }

    public static class RagAi {
        private String baseUrl = "http://localhost:8100";
        private int timeoutSeconds = 100;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Resilience {
        private Duration timeout = Duration.ofSeconds(15);
        private int maxAttempts = 2;
        private Duration initialBackoff = Duration.ofMillis(100);
        private int failureThreshold = 3;
        private Duration openDuration = Duration.ofSeconds(30);

        public Duration getTimeout() { return timeout; }
        public void setTimeout(Duration timeout) { this.timeout = timeout; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public Duration getInitialBackoff() { return initialBackoff; }
        public void setInitialBackoff(Duration initialBackoff) { this.initialBackoff = initialBackoff; }
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        public Duration getOpenDuration() { return openDuration; }
        public void setOpenDuration(Duration openDuration) { this.openDuration = openDuration; }
    }
}
