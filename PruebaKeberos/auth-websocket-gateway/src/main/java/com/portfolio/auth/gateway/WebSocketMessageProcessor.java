package com.portfolio.auth.gateway;

import java.util.Objects;

public final class WebSocketMessageProcessor {
    private final GatewayAuthFlowService flowService;

    public WebSocketMessageProcessor(GatewayAuthFlowService flowService) {
        this.flowService = Objects.requireNonNull(flowService, "flowService");
    }

    public WebSocketMessage process(WebSocketMessage input, WebSocketEventPublisher publisher) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(publisher, "publisher");

        return switch (input.type()) {
            case PING -> WebSocketMessage.pong(input.requestId());
            case START_AUTH_FLOW, RUN_AUDIT_FLOW -> flowService.run(input, publisher);
            default -> WebSocketMessage.error(input.requestId(), "Accion WebSocket no aceptada como entrada: "
                    + input.type());
        };
    }
}
