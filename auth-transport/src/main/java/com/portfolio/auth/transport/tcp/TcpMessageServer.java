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
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class TcpMessageServer implements AutoCloseable {
    public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);
    public static final int DEFAULT_MAX_MESSAGE_BYTES = 64 * 1024;

    private final String host;
    private final int requestedPort;
    private final JsonMessageCodec codec;
    private final MessageHandler handler;
    private final MessageType expectedRequestType;
    private final Duration readTimeout;
    private final int maxMessageBytes;
    private final ExecutorService acceptExecutor;
    private final ExecutorService handlerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private ServerSocket serverSocket;

    public TcpMessageServer(String host, int port, JsonMessageCodec codec, MessageHandler handler) {
        this(host, port, codec, handler, null, DEFAULT_READ_TIMEOUT, DEFAULT_MAX_MESSAGE_BYTES);
    }

    public TcpMessageServer(
            String host,
            int port,
            JsonMessageCodec codec,
            MessageHandler handler,
            MessageType expectedRequestType) {
        this(host, port, codec, handler, expectedRequestType, DEFAULT_READ_TIMEOUT, DEFAULT_MAX_MESSAGE_BYTES);
    }

    public TcpMessageServer(
            String host,
            int port,
            JsonMessageCodec codec,
            MessageHandler handler,
            MessageType expectedRequestType,
            Duration readTimeout,
            int maxMessageBytes) {
        this.host = Objects.requireNonNull(host, "host");
        this.requestedPort = port;
        this.codec = Objects.requireNonNull(codec, "codec");
        this.handler = Objects.requireNonNull(handler, "handler");
        this.expectedRequestType = expectedRequestType;
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException("maxMessageBytes debe ser positivo");
        }
        this.maxMessageBytes = maxMessageBytes;
        this.acceptExecutor = Executors.newSingleThreadExecutor();
        this.handlerExecutor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
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
            accepted.setSoTimeout(Math.toIntExact(readTimeout.toMillis()));

            String requestId = "tcp-error-" + UUID.randomUUID();
            try {
                String line = BoundedLineReader.readLine(reader, maxMessageBytes);
                if (line == null) {
                    return;
                }
                if (line.isBlank()) {
                    throw new IllegalArgumentException("JSON vacio");
                }

                ProtocolEnvelope request = codec.decodeEnvelope(line);
                requestId = request.requestId();
                if (expectedRequestType != null && request.messageType() != expectedRequestType) {
                    write(writer, error(
                            requestId,
                            "TRANSPORT_UNEXPECTED_MESSAGE",
                            "Tipo de mensaje no aceptado por este endpoint"));
                    return;
                }

                ProtocolEnvelope response = handler.handle(request);
                write(writer, response);
            } catch (Exception e) {
                write(writer, error(requestId, "TRANSPORT_ERROR", safeMessage(e)));
            }
        } catch (IOException e) {
            if (running.get()) {
                System.err.println("[tcp] Error atendiendo conexion modular: " + e.getMessage());
            }
        }
    }

    private void write(BufferedWriter writer, ProtocolEnvelope response) throws IOException {
        String encoded = codec.encode(response);
        BoundedLineReader.requireWithinLimit(encoded, maxMessageBytes);
        writer.write(encoded);
        writer.newLine();
        writer.flush();
    }

    private ProtocolEnvelope error(String requestId, String code, String message) {
        ErrorResponse error = new ErrorResponse(
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                "tcp",
                code,
                message,
                requestId);
        return new ProtocolEnvelope(
                MessageType.ERROR_RESPONSE,
                ProtocolDefaults.CURRENT_VERSION,
                requestId,
                Instant.now(),
                codec.encodePayload(error));
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message;
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
