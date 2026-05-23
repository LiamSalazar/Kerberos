package com.portfolio.auth.gateway;

import java.time.Duration;

final class ConnectionRateLimit {
    private final int maxMessages;
    private final long windowNanos;
    private long windowStartedNanos;
    private int messages;

    ConnectionRateLimit(int maxMessages, Duration window) {
        this.maxMessages = maxMessages;
        this.windowNanos = window.toNanos();
        this.windowStartedNanos = System.nanoTime();
    }

    synchronized boolean allow() {
        long now = System.nanoTime();
        if (now - windowStartedNanos > windowNanos) {
            windowStartedNanos = now;
            messages = 0;
        }
        messages++;
        return messages <= maxMessages;
    }
}
