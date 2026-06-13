package com.histar.be.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public class ChatProperties {

    private RagAi ragAi = new RagAi();

    public RagAi getRagAi() {
        return ragAi;
    }

    public void setRagAi(RagAi ragAi) {
        this.ragAi = ragAi;
    }

    public static class RagAi {
        private String baseUrl = "http://localhost:8100";
        private int timeoutSeconds = 120;

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
}
