package com.portfolio.auth.transport.tcp;

import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class TcpMessageClient {
    public static final int DEFAULT_MAX_MESSAGE_BYTES = 64 * 1024;

    private final JsonMessageCodec codec;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int maxMessageBytes;

    public TcpMessageClient(JsonMessageCodec codec) {
        this(codec, Duration.ofSeconds(5), Duration.ofSeconds(5), DEFAULT_MAX_MESSAGE_BYTES);
    }

    public TcpMessageClient(JsonMessageCodec codec, Duration connectTimeout, Duration readTimeout) {
        this(codec, connectTimeout, readTimeout, DEFAULT_MAX_MESSAGE_BYTES);
    }

    public TcpMessageClient(
            JsonMessageCodec codec,
            Duration connectTimeout,
            Duration readTimeout,
            int maxMessageBytes) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout");
        this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout");
        if (maxMessageBytes <= 0) {
            throw new IllegalArgumentException("maxMessageBytes debe ser positivo");
        }
        this.maxMessageBytes = maxMessageBytes;
    }

    public ProtocolEnvelope send(String host, int port, ProtocolEnvelope request) throws IOException {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(request, "request");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(connectTimeout.toMillis()));
            socket.setSoTimeout(Math.toIntExact(readTimeout.toMillis()));

            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String encodedRequest = codec.encode(request);
            BoundedLineReader.requireWithinLimit(encodedRequest, maxMessageBytes);
            writer.write(encodedRequest);
            writer.newLine();
            writer.flush();

            String response = BoundedLineReader.readLine(reader, maxMessageBytes);
            if (response == null) {
                throw new IOException("El servidor cerro la conexion sin respuesta");
            }
            return codec.decodeEnvelope(response);
        }
    }
}
