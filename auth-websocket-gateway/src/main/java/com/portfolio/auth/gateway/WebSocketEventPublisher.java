package com.portfolio.auth.gateway;

@FunctionalInterface
public interface WebSocketEventPublisher {
    void publish(WebSocketMessage message);
}
