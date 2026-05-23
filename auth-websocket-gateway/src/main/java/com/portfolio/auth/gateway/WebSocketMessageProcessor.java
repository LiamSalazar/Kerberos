package com.portfolio.auth.gateway;

import java.util.Objects;

public final class WebSocketMessageProcessor {
    private final GatewayAuthFlowService flowService;
    private final GatewaySessionService sessionService;

    public WebSocketMessageProcessor(GatewayAuthFlowService flowService, GatewaySessionService sessionService) {
        this.flowService = Objects.requireNonNull(flowService, "flowService");
        this.sessionService = Objects.requireNonNull(sessionService, "sessionService");
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
            case VERIFY_SESSION -> {
                WebSocketMessage error = validateSessionRequest(input, true);
                yield error == null ? sessionService.verify(input) : error;
            }
            case LOGOUT_SESSION -> {
                WebSocketMessage error = validateSessionRequest(input, false);
                yield error == null ? sessionService.logout(input) : error;
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
                case "sessionId" -> input.sessionId();
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

    private static WebSocketMessage validateSessionRequest(WebSocketMessage input, boolean requirePrincipalFields) {
        WebSocketMessage requestIdError = validateRequired(input, "requestId");
        if (requestIdError != null) {
            return requestIdError;
        }
        if (input.sessionId() == null || input.sessionId().isBlank()) {
            return WebSocketMessage.error(
                    input.requestId(),
                    WebSocketErrorType.SESSION_REQUIRED,
                    "Campo requerido faltante: sessionId");
        }
        if (requirePrincipalFields) {
            WebSocketMessage principalError = validateRequired(input, "clientId", "serviceId");
            if (principalError != null) {
                return WebSocketMessage.error(
                        input.requestId(),
                        WebSocketErrorType.INVALID_SESSION_REQUEST,
                        principalError.message());
            }
        }
        return null;
    }
}
