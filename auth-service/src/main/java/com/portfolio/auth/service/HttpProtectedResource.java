package com.portfolio.auth.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

public final class HttpProtectedResource implements ProtectedResource {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(3);

    private final String serviceId;
    private final URI endpoint;
    private final HttpClient httpClient;

    public HttpProtectedResource(String serviceId, URI endpoint) {
        this(serviceId, endpoint, HttpClient.newHttpClient());
    }

    public HttpProtectedResource(String serviceId, URI endpoint, HttpClient httpClient) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
    }

    @Override
    public String getServiceId() {
        return serviceId;
    }

    @Override
    public ProtectedServiceResponse execute(ProtectedServiceRequest request) {
        Objects.requireNonNull(request, "request");
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(DEFAULT_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson(request)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            boolean success = response.statusCode() >= 200 && response.statusCode() < 300;
            return new ProtectedServiceResponse(response.body(), success);
        } catch (IOException exception) {
            return new ProtectedServiceResponse("HTTP protected resource unavailable", false);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new ProtectedServiceResponse("HTTP protected resource interrupted", false);
        }
    }

    private static String requestJson(ProtectedServiceRequest request) {
        return "{"
                + "\"requestId\":" + quote(request.requestId()) + ","
                + "\"clientId\":" + quote(request.clientId()) + ","
                + "\"serviceId\":" + quote(request.serviceId()) + ","
                + "\"authenticatedAt\":" + quote(request.authenticatedAt().toString())
                + "}";
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(ch);
            }
        }
        return escaped.append('"').toString();
    }
}
