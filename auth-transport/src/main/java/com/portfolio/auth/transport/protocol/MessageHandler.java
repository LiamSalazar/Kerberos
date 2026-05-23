package com.portfolio.auth.transport.protocol;

@FunctionalInterface
public interface MessageHandler {
    ProtocolEnvelope handle(ProtocolEnvelope request) throws Exception;
}
