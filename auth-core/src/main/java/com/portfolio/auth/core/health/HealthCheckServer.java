package com.portfolio.auth.core.health;

import com.portfolio.auth.core.observability.MetricsRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class HealthCheckServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor;

    private HealthCheckServer(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    public static HealthCheckServer start(
            String host,
            int port,
            HealthCheckSupport support,
            MetricsRegistry metricsRegistry) throws IOException {
        Objects.requireNonNull(support, "support");
        Objects.requireNonNull(metricsRegistry, "metricsRegistry");
        if (port <= 0) {
            throw new IllegalArgumentException("health port must be positive");
        }
        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        server.setExecutor(executor);
        server.createContext("/health", exchange -> write(exchange, 200, "application/json", support.json()));
        server.createContext("/metrics", exchange -> write(exchange, 200, "text/plain", metricsRegistry.toPrometheusText()));
        server.createContext("/", exchange -> write(exchange, 200, "application/json", support.json()));
        server.start();
        return new HealthCheckServer(server, executor);
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    private static void write(HttpExchange exchange, int statusCode, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
