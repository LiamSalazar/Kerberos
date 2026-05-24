package com.portfolio.auth.core.observability;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

public final class MetricsRegistry {
    private static final MetricsRegistry GLOBAL = new MetricsRegistry();

    private final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LatencyMetric> latencies = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongSupplier> gauges = new ConcurrentHashMap<>();

    public static MetricsRegistry global() {
        return GLOBAL;
    }

    public void increment(String name) {
        counters.computeIfAbsent(metricName(name), ignored -> new AtomicLong()).incrementAndGet();
    }

    public void recordLatency(String name, long latencyMs) {
        latencies.computeIfAbsent(metricName(name), ignored -> new LatencyMetric())
                .record(Math.max(0, latencyMs));
    }

    public void gauge(String name, LongSupplier supplier) {
        gauges.put(metricName(name), Objects.requireNonNull(supplier, "supplier"));
    }

    public String toPrometheusText() {
        StringBuilder output = new StringBuilder();
        counters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> output.append(entry.getKey()).append(' ')
                        .append(entry.getValue().get()).append('\n'));
        latencies.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    LatencyMetric metric = entry.getValue();
                    output.append(entry.getKey()).append("_count ").append(metric.count()).append('\n');
                    output.append(entry.getKey()).append("_sum ").append(metric.sum()).append('\n');
                    output.append(entry.getKey()).append("_max ").append(metric.max()).append('\n');
                });
        gauges.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> output.append(entry.getKey()).append(' ')
                        .append(Math.max(0, entry.getValue().getAsLong())).append('\n'));
        return output.toString();
    }

    private static String metricName(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[a-zA-Z_:][a-zA-Z0-9_:]*")) {
            throw new IllegalArgumentException("Invalid metric name: " + name);
        }
        return name;
    }

    private static final class LatencyMetric {
        private final AtomicLong count = new AtomicLong();
        private final AtomicLong sum = new AtomicLong();
        private final AtomicLong max = new AtomicLong();

        void record(long value) {
            count.incrementAndGet();
            sum.addAndGet(value);
            max.updateAndGet(current -> Math.max(current, value));
        }

        long count() {
            return count.get();
        }

        long sum() {
            return sum.get();
        }

        long max() {
            return max.get();
        }
    }
}
