package com.histar.be.admin.service;

import com.histar.be.admin.dto.DemoActivitySnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DemoActivityService {
    private static final Duration ACTIVE_WINDOW = Duration.ofMinutes(5);
    private final Clock clock;
    private final Map<String, Instant> lastActivity = new ConcurrentHashMap<>();

    public DemoActivityService() {
        this(Clock.systemUTC());
    }

    public DemoActivityService(Clock clock) {
        this.clock = clock;
    }

    public void heartbeat(String actor) {
        if (actor != null && !actor.isBlank()) {
            lastActivity.put(actor, clock.instant());
        }
    }

    public DemoActivitySnapshot snapshot() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(ACTIVE_WINDOW);
        lastActivity.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        return new DemoActivitySnapshot(lastActivity.size(), ACTIVE_WINDOW.toSeconds(), now);
    }
}
