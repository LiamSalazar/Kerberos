package com.portfolio.auth.service;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.repository.InMemoryServiceRegistry;
import com.portfolio.auth.core.repository.ServiceRegistry;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.storage.sqlite.SQLiteServiceRegistry;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;

public final class ProtectedServiceApp {
    private ProtectedServiceApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        JsonMessageCodec codec = new JsonMessageCodec();
        ServiceRegistry registry = config.usesSqliteStorage()
                ? new SQLiteServiceRegistry(java.nio.file.Path.of(config.sqlitePath()))
                : InMemoryServiceRegistry.fromConfig(config);
        ProtectedServiceHandler handler = new ProtectedServiceHandler(
                config,
                registry,
                DemoProtectedResource.fromConfig(config),
                new InMemoryReplayCache(),
                codec,
                new SecureJsonCrypto(codec, new AesGcmCryptoService(), config.demoPbkdf2Salt()));
        TcpMessageServer server = new TcpMessageServer(
                config.serviceServerHost(),
                config.serviceServerPort(),
                codec,
                handler,
                MessageType.SERVICE_REQUEST);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("[auth-service] Servicio modular escuchando en " + config.serviceServerPort());
        Thread.currentThread().join();
    }
}
