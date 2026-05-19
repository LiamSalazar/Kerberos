package com.portfolio.auth.gateway;

import com.portfolio.auth.client.AuthClientException;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class GatewayAuthFlowService {
    private final AuthConfig config;
    private final GatewayAuthClient authClient;

    public GatewayAuthFlowService(AuthConfig config, GatewayAuthClient authClient) {
        this.config = Objects.requireNonNull(config, "config");
        this.authClient = Objects.requireNonNull(authClient, "authClient");
    }

    public WebSocketMessage run(WebSocketMessage input, WebSocketEventPublisher publisher) {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(publisher, "publisher");

        String requestId = requestId(input.requestId());
        String clientId = valueOrDefault(input.clientId(), authClient.configuredClientId());
        String serviceId = valueOrDefault(input.serviceId(), config.defaultServiceId());
        WebSocketAuthSession session = new WebSocketAuthSession(requestId, clientId, serviceId, Instant.now());

        long started = System.nanoTime();
        long asMillis = 0;
        long tgsMillis = 0;
        long serviceMillis = 0;

        publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.FLOW_STARTED,
                "Flujo modular iniciado"));

        if (!authClient.configuredClientId().equals(session.clientId())) {
            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.FLOW_ERROR,
                    "Cliente no registrado en la configuracion local"));
            return WebSocketMessage.flowResult(
                    session.requestId(),
                    false,
                    "Cliente no registrado en la configuracion local",
                    asMillis,
                    tgsMillis,
                    serviceMillis,
                    elapsedMillis(started));
        }

        try {
            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.AS_REQUEST_SENT,
                    "Solicitud AS enviada"));
            long asStarted = System.nanoTime();
            SecureAsResponse asResponse = authClient.requestTicketGrantingTicket("ws-as-" + session.requestId());
            asMillis = elapsedMillis(asStarted);
            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.AS_RESPONSE_RECEIVED,
                    "TGT emitido"));

            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.TGS_REQUEST_SENT,
                    "Solicitud TGS enviada"));
            long tgsStarted = System.nanoTime();
            SecureTgsResponse tgsResponse = authClient.requestServiceTicket(
                    asResponse,
                    session.serviceId(),
                    "ws-tgs-" + session.requestId(),
                    "ws-auth-tgs-" + session.requestId(),
                    Instant.now());
            tgsMillis = elapsedMillis(tgsStarted);
            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.TGS_RESPONSE_RECEIVED,
                    "Ticket de servicio emitido"));

            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.SERVICE_REQUEST_SENT,
                    "Solicitud de servicio enviada"));
            long serviceStarted = System.nanoTime();
            ServiceResponse serviceResponse = authClient.requestProtectedService(
                    tgsResponse,
                    "ws-service-" + session.requestId(),
                    "ws-auth-service-" + session.requestId(),
                    Instant.now());
            serviceMillis = elapsedMillis(serviceStarted);
            publisher.publish(WebSocketMessage.flowEvent(
                    session.requestId(),
                    WebSocketFlowStage.SERVICE_RESPONSE_RECEIVED,
                    "Respuesta de servicio recibida"));

            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.FLOW_SUCCESS,
                    "Flujo modular completado"));
            return WebSocketMessage.flowResult(
                    session.requestId(),
                    serviceResponse.accessGranted(),
                    serviceResponse.serviceMessage(),
                    asMillis,
                    tgsMillis,
                    serviceMillis,
                    elapsedMillis(started));
        } catch (Exception e) {
            String message = safeMessage(e);
            publisher.publish(WebSocketMessage.flowEvent(session.requestId(), WebSocketFlowStage.FLOW_ERROR, message));
            return WebSocketMessage.flowResult(
                    session.requestId(),
                    false,
                    message,
                    asMillis,
                    tgsMillis,
                    serviceMillis,
                    elapsedMillis(started));
        }
    }

    private static String requestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return "ws-" + UUID.randomUUID();
        }
        return requestId;
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0, (System.nanoTime() - startedNanos) / 1_000_000);
    }

    private static String safeMessage(Exception e) {
        if (e instanceof AuthClientException authError) {
            return authError.errorResponse().errorCode() + ": " + authError.errorResponse().errorMessage();
        }
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getClass().getSimpleName() + ": " + message;
    }
}
