package com.portfolio.auth.gateway;

import com.portfolio.auth.core.session.InMemorySessionRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GatewaySessionServiceTest {

    @Test
    void shouldCreateSessionNoLongerThanServiceTicket() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        GatewaySessionService service = new GatewaySessionService(
                repository,
                Duration.ofMinutes(5),
                Duration.ofMinutes(10),
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "session-1");

        var session = service.createSession(
                "request-1",
                "client-1",
                "service-1",
                now.plusSeconds(45));

        assertEquals("session-1", session.sessionId());
        assertEquals(now.plusSeconds(45), session.expiresAt());
        assertTrue(repository.findById("session-1").isPresent());
    }

    @Test
    void shouldVerifyExpiredClientMismatchServiceMismatchAndLogout() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        GatewaySessionService service = new GatewaySessionService(
                repository,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "session-1");
        service.createSession("request-1", "client-1", "service-1", now.plusSeconds(30));

        assertEquals(WebSocketMessageType.SESSION_VALID,
                service.verify(WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-ok",
                        "session-1",
                        "client-1",
                        "service-1")).type());

        assertEquals("CLIENT_MISMATCH",
                service.verify(WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-client",
                        "session-1",
                        "other-client",
                        "service-1")).reason());

        assertEquals("SERVICE_MISMATCH",
                service.verify(WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-service",
                        "session-1",
                        "client-1",
                        "other-service")).reason());

        assertEquals(WebSocketMessageType.SESSION_LOGGED_OUT,
                service.logout(WebSocketMessage.sessionInbound(
                        WebSocketMessageType.LOGOUT_SESSION,
                        "logout-1",
                        "session-1",
                        null,
                        null)).type());

        assertEquals("REVOKED",
                service.verify(WebSocketMessage.sessionInbound(
                        WebSocketMessageType.VERIFY_SESSION,
                        "verify-revoked",
                        "session-1",
                        "client-1",
                        "service-1")).reason());
    }

    @Test
    void shouldReportExpiredSession() {
        Instant now = Instant.parse("2026-05-23T00:00:00Z");
        InMemorySessionRepository repository = new InMemorySessionRepository();
        GatewaySessionService creator = new GatewaySessionService(
                repository,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Clock.fixed(now, ZoneOffset.UTC),
                () -> "session-1");
        creator.createSession("request-1", "client-1", "service-1", now.plusSeconds(5));

        GatewaySessionService verifier = new GatewaySessionService(
                repository,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Clock.fixed(now.plusSeconds(5), ZoneOffset.UTC),
                () -> "session-2");

        WebSocketMessage invalid = verifier.verify(WebSocketMessage.sessionInbound(
                WebSocketMessageType.VERIFY_SESSION,
                "verify-expired",
                "session-1",
                "client-1",
                "service-1"));

        assertEquals(WebSocketMessageType.SESSION_INVALID, invalid.type());
        assertEquals("EXPIRED", invalid.reason());
    }
}
