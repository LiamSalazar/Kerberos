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
            case PING -> {
                WebSocketMessage error = validateRequired(input, "requestId");
                yield error == null ? WebSocketMessage.pong(input.requestId()) : error;
            }
            case START_AUTH_FLOW -> {
                WebSocketMessage error = validateRequired(input, "requestId", "clientId", "serviceId");
                yield error == null ? flowService.run(input, publisher) : error;
            }
            default -> WebSocketMessage.error(
                    input.requestId(),
                    WebSocketErrorType.UNKNOWN_MESSAGE_TYPE,
                    "Tipo de mensaje WebSocket no aceptado como entrada: " + input.type());
        };
    }

    private static WebSocketMessage validateRequired(WebSocketMessage input, String... fields) {
        for (String field : fields) {
            String value = switch (field) {
                case "requestId" -> input.requestId();
                case "clientId" -> input.clientId();
                case "serviceId" -> input.serviceId();
                default -> throw new IllegalArgumentException("Campo requerido no soportado: " + field);
            };
            if (value == null || value.isBlank()) {
                return WebSocketMessage.error(
                        input.requestId(),
                        WebSocketErrorType.MISSING_REQUIRED_FIELD,
                        "Campo requerido faltante: " + field);
            }
        }
        return null;
    }
}
