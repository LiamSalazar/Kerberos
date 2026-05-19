package com.portfolio.auth.gateway;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AuthWebSocketServer extends WebSocketServer {
    private final WebSocketMessageCodec codec;
    private final WebSocketMessageProcessor processor;
    private final ExecutorService executor;

    public AuthWebSocketServer(
            InetSocketAddress address,
            WebSocketMessageCodec codec,
            WebSocketMessageProcessor processor) {
        super(address);
        this.codec = Objects.requireNonNull(codec, "codec");
        this.processor = Objects.requireNonNull(processor, "processor");
        this.executor = Executors.newCachedThreadPool();
        setConnectionLostTimeout(30);
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        connection.send(codec.encode(WebSocketMessage.ready()));
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        executor.submit(() -> handleMessage(connection, message));
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        // No session state is retained after each request in this gateway.
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

    private void handleMessage(WebSocket connection, String rawMessage) {
        try {
            WebSocketMessage input = codec.decode(rawMessage);
            WebSocketMessage output = processor.process(input, event -> connection.send(codec.encode(event)));
            if (output != null) {
                connection.send(codec.encode(output));
            }
        } catch (Exception e) {
            connection.send(codec.encode(WebSocketMessage.error(null, safeMessage(e))));
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return message;
    }
}
