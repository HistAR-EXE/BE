package com.histar.be.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class DemoActivityServiceTest {

    @Test
    void countsDistinctActorsActiveWithinFiveMinutes() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));
        Object service = newService(clock);
        Method heartbeat = service.getClass().getMethod("heartbeat", String.class);
        heartbeat.invoke(service, "user-1");
        heartbeat.invoke(service, "user-1");
        heartbeat.invoke(service, "user-2");

        Object snapshot = service.getClass().getMethod("snapshot").invoke(service);

        assertThat(snapshot.getClass().getMethod("activeUsers").invoke(snapshot)).isEqualTo(2);
        assertThat(snapshot.getClass().getMethod("windowSeconds").invoke(snapshot)).isEqualTo(300L);
    }

    @Test
    void dropsActorsAfterTheActivityWindowExpires() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-10-26T10:00:00Z"));
        Object service = newService(clock);
        service.getClass().getMethod("heartbeat", String.class).invoke(service, "user-1");
        clock.advanceSeconds(301);

        Object snapshot = service.getClass().getMethod("snapshot").invoke(service);

        assertThat(snapshot.getClass().getMethod("activeUsers").invoke(snapshot)).isEqualTo(0);
    }

    private Object newService(Clock clock) throws Exception {
        try {
            Class<?> type = Class.forName("com.histar.be.admin.service.DemoActivityService");
            return type.getConstructor(Clock.class).newInstance(clock);
        } catch (ClassNotFoundException exception) {
            fail("DemoActivityService is required for safe demo shutdown");
            return null;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
