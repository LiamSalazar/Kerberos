package com.portfolio.auth.gateway;

import com.portfolio.auth.client.AuthClient;
import com.portfolio.auth.core.config.AuthConfig;

import java.net.InetSocketAddress;

public final class WebSocketGatewayApp {
    public static final String ENV_WEBSOCKET_HOST = "AUTH_WS_HOST";
    public static final String ENV_WEBSOCKET_PORT = "AUTH_WS_PORT";
    public static final int DEFAULT_WEBSOCKET_PORT = 2800;

    private WebSocketGatewayApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        String host = value(ENV_WEBSOCKET_HOST, AuthConfig.DEFAULT_LOCAL_HOST);
        int port = intValue(ENV_WEBSOCKET_PORT, DEFAULT_WEBSOCKET_PORT);

        AuthClient authClient = new AuthClient(config);
        GatewayAuthFlowService flowService = new GatewayAuthFlowService(
                config,
                new DefaultGatewayAuthClient(config, authClient));
        AuthWebSocketServer server = new AuthWebSocketServer(
                new InetSocketAddress(host, port),
                new WebSocketMessageCodec(),
                new WebSocketMessageProcessor(flowService));

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        Thread.currentThread().join();
    }

    private static String value(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static int intValue(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
