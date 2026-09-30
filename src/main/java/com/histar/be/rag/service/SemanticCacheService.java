package com.histar.be.rag.service;

import com.histar.be.rag.config.RagProperties;
import com.histar.be.rag.repository.SemanticCacheRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Very simple semantic cache: near-identical question embeddings (same station + persona) reuse a reply. */
@Service
@RequiredArgsConstructor
public class SemanticCacheService {

    private final SemanticCacheRepository repository;
    private final RagProperties ragProperties;

    public boolean isEnabled() {
        return ragProperties.getCache().isEnabled();
    }

    public Optional<String> lookup(String vec, String stationCode, String persona) {
        if (!isEnabled() || vec == null) {
            return Optional.empty();
        }
        List<Object[]> rows = repository.findNearest(vec, persona, normalize(stationCode));
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        Object[] row = rows.get(0);
        double distance = ((Number) row[1]).doubleValue();
        if (distance > ragProperties.getCache().getMaxDistance()) {
            return Optional.empty();
        }
        return Optional.ofNullable((String) row[0]);
    }

    public void store(String vec, String stationCode, String persona, String reply) {
        if (!isEnabled() || vec == null || reply == null || reply.isBlank()) {
            return;
        }
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(ragProperties.getCache().getTtlMinutes()));
        String station = normalize(stationCode);
        repository.insert(UUID.randomUUID(), vec, station.isEmpty() ? null : station, persona, reply, expiresAt);
    }

    private static String normalize(String stationCode) {
        return stationCode == null ? "" : stationCode.trim();
    }
}
