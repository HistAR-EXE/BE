package com.histar.be.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * pgvector RAG settings ({@code rag.*}). Disabled by default ({@code RAG_ENABLED=false}): the chat path then
 * behaves exactly as before.
 */
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    private boolean enabled = false;
    /** Max chunks injected into the prompt. */
    private int topK = 4;
    /** Cosine distance (0 = identical, 2 = opposite). Chunks farther than this are ignored. */
    private double maxDistance = 0.6;
    private Embedding embedding = new Embedding();
    private Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public static class Embedding {
        /** Gemini embedding model id, e.g. gemini-embedding-001 or text-embedding-004. */
        private String model = "gemini-embedding-001";
        /** Must match the vector(768) column. Sent as outputDimensionality. */
        private int dimensions = 768;
        private int timeoutSeconds = 15;

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getDimensions() {
            return dimensions;
        }

        public void setDimensions(int dimensions) {
            this.dimensions = dimensions;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Cache {
        private boolean enabled = false;
        private int ttlMinutes = 1440;
        /** Max cosine distance for a cache hit (very strict: near-identical questions only). */
        private double maxDistance = 0.05;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTtlMinutes() {
            return ttlMinutes;
        }

        public void setTtlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
        }

        public double getMaxDistance() {
            return maxDistance;
        }

        public void setMaxDistance(double maxDistance) {
            this.maxDistance = maxDistance;
        }
    }
}
