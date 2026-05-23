package com.portfolio.auth.gateway;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AuthWebSocketServer extends WebSocketServer {
    private final WebSocketMessageCodec codec;
    private final WebSocketMessageProcessor processor;
    private final ExecutorService executor;
    private final WebSocketGatewayPolicy policy;
    private final ConcurrentMap<WebSocket, ConnectionRateLimit> rateLimits;

    public AuthWebSocketServer(
            InetSocketAddress address,
            WebSocketMessageCodec codec,
            WebSocketMessageProcessor processor) {
        this(address, codec, processor, WebSocketGatewayPolicy.defaults());
    }

    public AuthWebSocketServer(
            InetSocketAddress address,
            WebSocketMessageCodec codec,
            WebSocketMessageProcessor processor,
            WebSocketGatewayPolicy policy) {
        super(address);
        this.codec = Objects.requireNonNull(codec, "codec");
        this.processor = Objects.requireNonNull(processor, "processor");
        this.policy = Objects.requireNonNull(policy, "policy");
        this.executor = Executors.newCachedThreadPool();
        this.rateLimits = new ConcurrentHashMap<>();
        setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String origin = handshake.getFieldValue("Origin");
        if (!policy.allowsOrigin(origin)) {
            connection.send(codec.encode(WebSocketMessage.error(
                    null,
                    WebSocketErrorType.ORIGIN_NOT_ALLOWED,
                    "Origen WebSocket no permitido")));
            connection.close(1008, "origin not allowed");
            return;
        }
        rateLimits.put(connection, new ConnectionRateLimit(
                policy.maxMessagesPerConnection(),
                policy.rateLimitWindow()));
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        ConnectionRateLimit rateLimit = rateLimits.computeIfAbsent(
                connection,
                ignored -> new ConnectionRateLimit(
                        policy.maxMessagesPerConnection(),
                        policy.rateLimitWindow()));
        if (!rateLimit.allow()) {
            connection.send(codec.encode(WebSocketMessage.error(
                    null,
                    WebSocketErrorType.RATE_LIMITED,
                    "Rate limit WebSocket excedido para esta conexion")));
            return;
        }
        executor.submit(() -> handleMessageWithTimeout(connection, message));
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        rateLimits.remove(connection);
    }

    @Override
    public void onError(WebSocket connection, Exception exception) {
        String message = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        System.err.println("[auth-websocket-gateway] Error WebSocket controlado: " + message);
    }

    @Override
    public void onStart() {
        System.out.println("[auth-websocket-gateway] Escuchando en " + getAddress());
    }

    public void shutdown() {
        executor.shutdownNow();
        try {
            stop(1_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleMessage(WebSocket connection, String rawMessage, AtomicBoolean active) {
        try {
            WebSocketMessage input = codec.decode(rawMessage);
            WebSocketMessage output = processor.process(input, event -> sendIfActive(connection, event, active));
            if (output != null) {
                sendIfActive(connection, output, active);
            }
        } catch (Exception e) {
            sendIfActive(connection, WebSocketMessage.error(null, errorType(e), safeMessage(e)), active);
        }
    }

    private void handleMessageWithTimeout(WebSocket connection, String rawMessage) {
        AtomicBoolean active = new AtomicBoolean(true);
        Future<?> task = executor.submit(() -> handleMessage(connection, rawMessage, active));
        try {
            task.get(policy.flowTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            active.set(false);
            task.cancel(true);
            connection.send(codec.encode(WebSocketMessage.error(
                    null,
                    WebSocketErrorType.FLOW_FAILED,
                    "Timeout de flujo WebSocket")));
        } catch (Exception e) {
            active.set(false);
            connection.send(codec.encode(WebSocketMessage.error(null, errorType(e), safeMessage(e))));
        }
    }

    private void sendIfActive(WebSocket connection, WebSocketMessage message, AtomicBoolean active) {
        if (active.get()) {
            connection.send(codec.encode(message));
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }

    private static WebSocketErrorType errorType(Exception e) {
        String message = e.getMessage() == null ? "" : e.getMessage();
        if (message.contains("WebSocketMessageType no soportado")) {
            return WebSocketErrorType.UNKNOWN_MESSAGE_TYPE;
        }
        if (message.contains("Falta string JSON type")) {
            return WebSocketErrorType.MISSING_REQUIRED_FIELD;
        }
        return WebSocketErrorType.INVALID_JSON;
    }
}
