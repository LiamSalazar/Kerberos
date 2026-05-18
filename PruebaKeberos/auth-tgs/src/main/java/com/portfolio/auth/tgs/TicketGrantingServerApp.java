package com.portfolio.auth.tgs;

import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.replay.InMemoryReplayCache;
import com.portfolio.auth.crypto.AesGcmCryptoService;
import com.portfolio.auth.transport.json.JsonMessageCodec;
import com.portfolio.auth.transport.secure.SecureJsonCrypto;
import com.portfolio.auth.transport.tcp.TcpMessageServer;

public final class TicketGrantingServerApp {
    private TicketGrantingServerApp() {
    }

    public static void main(String[] args) throws Exception {
        AuthConfig config = AuthConfig.fromEnvironment();
        JsonMessageCodec codec = new JsonMessageCodec();
        TicketGrantingHandler handler = new TicketGrantingHandler(
                config,
                InMemoryServiceRegistry.fromConfig(config),
                new InMemoryReplayCache(),
                codec,
                new SecureJsonCrypto(codec, new AesGcmCryptoService(), config.legacyPbkdf2Salt()));
        TcpMessageServer server = new TcpMessageServer(
                config.ticketGrantingServerHost(),
                config.ticketGrantingServerPort(),
                codec,
                handler);

        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        System.out.println("[auth-tgs] Modular TGS escuchando en " + config.ticketGrantingServerPort());
        Thread.currentThread().join();
    }
}
