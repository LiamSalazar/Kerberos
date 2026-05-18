package com.portfolio.auth.transport.tcp;

import com.portfolio.auth.core.protocol.ProtocolDefaults;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageHandler;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpMessageServer implements AutoCloseable {
    private final String host;
    private final int requestedPort;
    private final JsonMessageCodec codec;
    private final MessageHandler handler;
    private final ExecutorService acceptExecutor;
    private final ExecutorService handlerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;

    public TcpMessageServer(String host, int port, JsonMessageCodec codec, MessageHandler handler) {
        this.host = Objects.requireNonNull(host, "host");
        this.requestedPort = port;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.acceptExecutor = Executors.newSingleThreadExecutor();
        this.handlerExecutor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(host, requestedPort));
        acceptExecutor.submit(this::acceptLoop);
    }

    public int port() {
        if (serverSocket == null) {
            return requestedPort;
        }
        return serverSocket.getLocalPort();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                handlerExecutor.submit(() -> handle(socket));
            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("[tcp] Error aceptando conexion modular: " + e.getMessage());
                }
            }
        }
    }

    private void handle(Socket socket) {
        try (Socket accepted = socket;
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(accepted.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(accepted.getOutputStream(), StandardCharsets.UTF_8))) {

            String line = reader.readLine();
            if (line == null || line.isBlank()) {
                return;
            }

            ProtocolEnvelope request = codec.decodeEnvelope(line);
            ProtocolEnvelope response = handler.handle(request);
            writer.write(codec.encode(response));
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            writeError(socket, e);
        }
    }

    private void writeError(Socket socket, Exception exception) {
        if (socket.isClosed()) {
            return;
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
            String requestId = "tcp-error-" + UUID.randomUUID();
            ErrorResponse error = new ErrorResponse(
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    Instant.now(),
                    "tcp",
                    "TRANSPORT_ERROR",
                    exception.getMessage(),
                    requestId);
            ProtocolEnvelope envelope = new ProtocolEnvelope(
                    MessageType.ERROR_RESPONSE,
                    ProtocolDefaults.CURRENT_VERSION,
                    requestId,
                    Instant.now(),
                    codec.encodePayload(error));
            writer.write(codec.encode(envelope));
            writer.newLine();
            writer.flush();
        } catch (IOException ignored) {
            // La conexion ya no puede reportar el fallo.
        }
    }

    @Override
    public void close() {
        running.set(false);
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // Close best-effort.
            }
        }
        acceptExecutor.shutdownNow();
        handlerExecutor.shutdownNow();
        awaitTermination(acceptExecutor);
        awaitTermination(handlerExecutor);
    }

    private static void awaitTermination(ExecutorService executor) {
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
