package com.portfolio.auth.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpProtectedResourceTest {
    @Test
    void shouldCallLocalHttpResourceWithoutExposingSecrets() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            server.createContext("/protected", exchange -> {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(requestBody.contains("\"requestId\":\"req-http\""));
                assertTrue(requestBody.contains("\"clientId\":\"client-http\""));
                assertTrue(requestBody.contains("\"serviceId\":\"service-http\""));
                byte[] response = "Protected HTTP resource granted".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();

            HttpProtectedResource resource = new HttpProtectedResource(
                    "service-http",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/protected"));

            ProtectedServiceResponse response = resource.execute(new ProtectedServiceRequest(
                    "req-http",
                    "client-http",
                    "service-http",
                    Instant.now()));

            assertTrue(response.accessGranted());
            assertEquals("Protected HTTP resource granted", response.serviceMessage());
        } finally {
            server.stop(0);
        }
    }
}
