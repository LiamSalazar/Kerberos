package com.portfolio.auth.core.replay;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Replay cache local en memoria para la demo y pruebas unitarias.
 */
public final class InMemoryReplayCache implements ReplayCache {
    private final ConcurrentMap<String, Instant> entries = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemoryReplayCache() {
        this(Clock.systemUTC());
    }

    public InMemoryReplayCache(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public boolean registerIfAbsent(String replayKey, Instant expiresAt) {
        Objects.requireNonNull(replayKey, "replayKey");
        Objects.requireNonNull(expiresAt, "expiresAt");

        Instant now = Instant.now(clock);
        if (!expiresAt.isAfter(now)) {
            return false;
        }

        AtomicBoolean registered = new AtomicBoolean(false);
        entries.compute(replayKey, (key, existingExpiration) -> {
            if (existingExpiration == null || !existingExpiration.isAfter(now)) {
                registered.set(true);
                return expiresAt;
            }
            return existingExpiration;
        });
        purgeExpired();
        return registered.get();
    }

    @Override
    public boolean contains(String replayKey) {
        Objects.requireNonNull(replayKey, "replayKey");

        purgeExpired();
        return entries.containsKey(replayKey);
    }

    @Override
    public void purgeExpired() {
        Instant now = Instant.now(clock);
        entries.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
