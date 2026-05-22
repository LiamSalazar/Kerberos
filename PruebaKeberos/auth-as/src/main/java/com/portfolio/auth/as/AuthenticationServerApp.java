package com.portfolio.auth.as;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.repository.PrincipalRepository;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.storage.sqlite.SQLitePrincipalRepository;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;

public final class AuthenticationServerApp {
    private AuthenticationServerApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        JsonMessageCodec codec = new JsonMessageCodec();
        PrincipalRepository principals = config.usesSqliteStorage()
                ? new SQLitePrincipalRepository(java.nio.file.Path.of(config.sqlitePath()))
                : InMemoryPrincipalRepository.fromConfig(config);
        AuthenticationHandler handler = new AuthenticationHandler(
                config,
                principals,
                codec,
                new SecureJsonCrypto(codec, new AesGcmCryptoService(), config.demoPbkdf2Salt()));
        TcpMessageServer server = new TcpMessageServer(
                config.authenticationServerHost(),
                config.authenticationServerPort(),
                codec,
                handler,
                MessageType.AS_REQUEST);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("[auth-as] Modular AS escuchando en " + config.authenticationServerPort());
        Thread.currentThread().join();
    }
}
