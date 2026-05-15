package com.portfolio.auth.core.replay;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

        purgeExpired();
        Instant now = Instant.now(clock);
        if (!expiresAt.isAfter(now)) {
            return false;
        }

        return entries.putIfAbsent(replayKey, expiresAt) == null;
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
