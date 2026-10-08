package com.histar.be.chat.memory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JourneyMemoryService {

    private static final long RETENTION_DAYS = 90;
    private final JourneyMemoryRepository repository;

    @Transactional(readOnly = true)
    public Optional<JourneyMemory> findActive(UUID userId, UUID locationId) {
        return repository.findByUserIdAndLocationId(userId, locationId)
                .filter(JourneyMemory::isEnabled)
                .filter(memory -> memory.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public JourneyMemory remember(
            UUID userId, UUID locationId, String lastVisitedPoi, String summary, String locale, String preferredMode) {
        JourneyMemory memory = repository.findByUserIdAndLocationId(userId, locationId)
                .orElseGet(() -> JourneyMemory.builder().userId(userId).locationId(locationId).build());
        Instant now = Instant.now();
        memory.setEnabled(true);
        memory.setLastVisitedPoi(truncate(lastVisitedPoi, 255));
        memory.setJourneySummary(truncate(summary, 1000));
        memory.setLocale(truncate(locale, 16));
        memory.setPreferredMode(truncate(preferredMode, 32));
        memory.setUpdatedAt(now);
        memory.setExpiresAt(now.plus(RETENTION_DAYS, ChronoUnit.DAYS));
        return repository.save(memory);
    }

    /** Updates default memory, but honors an explicit visitor opt-out. */
    @Transactional
    public void observe(
            UUID userId, UUID locationId, String lastVisitedPoi, String summary, String locale, String preferredMode) {
        var existing = repository.findByUserIdAndLocationId(userId, locationId);
        if (existing.isPresent() && !existing.get().isEnabled()) {
            return;
        }
        JourneyMemory memory = existing.orElseGet(() -> JourneyMemory.builder().userId(userId).locationId(locationId).build());
        Instant now = Instant.now();
        memory.setEnabled(true);
        memory.setLastVisitedPoi(truncate(lastVisitedPoi, 255));
        memory.setJourneySummary(truncate(summary, 1000));
        memory.setLocale(truncate(locale, 16));
        memory.setPreferredMode(truncate(preferredMode, 32));
        memory.setUpdatedAt(now);
        memory.setExpiresAt(now.plus(RETENTION_DAYS, ChronoUnit.DAYS));
        repository.save(memory);
    }

    @Transactional
    public JourneyMemory setEnabled(UUID userId, UUID locationId, boolean enabled) {
        JourneyMemory memory = repository.findByUserIdAndLocationId(userId, locationId)
                .orElseGet(() -> JourneyMemory.disabled(userId, locationId));
        memory.setEnabled(enabled);
        memory.setUpdatedAt(Instant.now());
        if (enabled && !memory.getExpiresAt().isAfter(Instant.now())) {
            memory.setExpiresAt(Instant.now().plus(RETENTION_DAYS, ChronoUnit.DAYS));
        }
        return repository.save(memory);
    }

    @Transactional
    public void deleteForLocation(UUID userId, UUID locationId) {
        repository.deleteByUserIdAndLocationId(userId, locationId);
    }

    @Transactional
    public void deleteAll(UUID userId) {
        repository.deleteByUserId(userId);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
