package com.portfolio.auth.core.replay;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryReplayCacheTest {

    @Test
    void shouldRejectKeyAlreadyRegisteredBeforeExpiration() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T10:00:00Z"));
        InMemoryReplayCache cache = new InMemoryReplayCache(clock);

        assertTrue(cache.registerIfAbsent("service:client-1:req-1", Instant.parse("2026-05-12T10:05:00Z")));
        assertFalse(cache.registerIfAbsent("service:client-1:req-1", Instant.parse("2026-05-12T10:05:00Z")));
        assertTrue(cache.contains("service:client-1:req-1"));
    }

    @Test
    void shouldAllowKeyAgainAfterExpiration() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T10:00:00Z"));
        InMemoryReplayCache cache = new InMemoryReplayCache(clock);

        assertTrue(cache.registerIfAbsent("tgs:client-1:req-1", Instant.parse("2026-05-12T10:01:00Z")));

        clock.setInstant(Instant.parse("2026-05-12T10:01:01Z"));

        assertFalse(cache.contains("tgs:client-1:req-1"));
        assertTrue(cache.registerIfAbsent("tgs:client-1:req-1", Instant.parse("2026-05-12T10:02:00Z")));
    }

    @Test
    void shouldNotRegisterAlreadyExpiredKey() {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T10:00:00Z"));
        InMemoryReplayCache cache = new InMemoryReplayCache(clock);

        assertFalse(cache.registerIfAbsent("expired", Instant.parse("2026-05-12T09:59:59Z")));
        assertFalse(cache.contains("expired"));
    }

    @Test
    void shouldRegisterOnlyOneConcurrentUseOfSameKey() throws Exception {
        MutableClock clock = new MutableClock(Instant.parse("2026-05-12T10:00:00Z"));
        InMemoryReplayCache cache = new InMemoryReplayCache(clock);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Callable<Boolean>> attempts = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            attempts.add(() -> {
                start.await();
                return cache.registerIfAbsent("service:client-1:auth-1", Instant.parse("2026-05-12T10:05:00Z"));
            });
        }

        try {
            List<Future<Boolean>> results = attempts.stream()
                    .map(executor::submit)
                    .toList();

            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    accepted++;
                }
            }

            assertEquals(1, accepted);
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
