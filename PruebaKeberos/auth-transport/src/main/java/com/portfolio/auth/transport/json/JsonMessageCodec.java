package com.portfolio.auth.transport.json;

import com.portfolio.auth.core.protocol.dto.AsRequest;
import com.portfolio.auth.core.protocol.dto.AsResponse;
import com.portfolio.auth.core.protocol.dto.ClientAuthenticator;
import com.portfolio.auth.core.protocol.dto.ErrorResponse;
import com.portfolio.auth.core.protocol.dto.ServiceRequest;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.core.protocol.dto.TgsRequest;
import com.portfolio.auth.core.protocol.dto.TgsResponse;
import com.portfolio.auth.core.protocol.dto.TicketService;
import com.portfolio.auth.core.protocol.dto.TicketTgs;
import com.portfolio.auth.crypto.CryptoEnvelope;
import com.portfolio.auth.transport.protocol.MessageType;
import com.portfolio.auth.transport.protocol.ProtocolEnvelope;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureServiceRequest;
import com.portfolio.auth.transport.secure.SecureTgsRequest;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Codec JSON minimo y deliberadamente acotado a los DTOs del protocolo.
 */
public final class JsonMessageCodec {

    public String encode(ProtocolEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");

        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("messageType", envelope.messageType().name());
        fields.put("version", envelope.version());
        fields.put("requestId", envelope.requestId());
        fields.put("issuedAt", envelope.issuedAt().toString());

        String payloadJson = envelope.payloadJson().isBlank() ? "{}" : envelope.payloadJson();
        return withoutTrailingObject(fields) + ",\"payload\":" + payloadJson + "}";
    }

    public ProtocolEnvelope decodeEnvelope(String json) {
        Map<String, Object> fields = object(json);
        if (!fields.containsKey("payload") || fields.get("payload") == null) {
            throw new IllegalArgumentException("Falta payload JSON");
        }
        return new ProtocolEnvelope(
                messageType(fields),
                string(fields, "version"),
                string(fields, "requestId"),
                instant(fields, "issuedAt"),
                toJsonValue(fields.get("payload")));
    }

    public String encodePayload(Object payload) {
        return toJsonValue(payloadToMap(payload));
    }

    public <T> T decodePayload(String json, Class<T> type) {
        Objects.requireNonNull(type, "type");
        Map<String, Object> fields = object(json);
        Object decoded;

        if (type == AsRequest.class) {
            decoded = new AsRequest(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    string(fields, "clientId"),
                    string(fields, "ticketGrantingServerId"),
                    instant(fields, "issuedAt"));
        } else if (type == AsResponse.class) {
            decoded = new AsResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    instant(fields, "expiresAt"),
                    string(fields, "clientId"),
                    string(fields, "ticketGrantingServerId"),
                    string(fields, "clientTgsSessionKey"),
                    ticketTgs(mapOrNull(fields, "ticketTgs")));
        } else if (type == TgsRequest.class) {
            decoded = new TgsRequest(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    string(fields, "ticketGrantingServerId"),
                    ticketTgs(mapOrNull(fields, "ticketTgs")),
                    authenticator(mapOrNull(fields, "clientAuthenticator")));
        } else if (type == TgsResponse.class) {
            decoded = new TgsResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    instant(fields, "expiresAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    string(fields, "clientServiceSessionKey"),
                    ticketService(mapOrNull(fields, "ticketService")));
        } else if (type == ServiceRequest.class) {
            decoded = new ServiceRequest(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    ticketService(mapOrNull(fields, "ticketService")),
                    authenticator(mapOrNull(fields, "clientAuthenticator")));
        } else if (type == ServiceResponse.class) {
            decoded = new ServiceResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    instant(fields, "expiresAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    instant(fields, "clientAuthenticatorIssuedAt"),
                    instant(fields, "serverValidatedAt"),
                    string(fields, "serviceMessage"),
                    bool(fields, "accessGranted"));
        } else if (type == TicketTgs.class) {
            decoded = ticketTgs(fields);
        } else if (type == TicketService.class) {
            decoded = ticketService(fields);
        } else if (type == ClientAuthenticator.class) {
            decoded = authenticator(fields);
        } else if (type == ErrorResponse.class) {
            decoded = new ErrorResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    string(fields, "source"),
                    string(fields, "errorCode"),
                    string(fields, "errorMessage"),
                    string(fields, "failedRequestId"));
        } else if (type == CryptoEnvelope.class) {
            decoded = cryptoEnvelope(fields);
        } else if (type == SecureAsResponse.class) {
            decoded = new SecureAsResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    instant(fields, "expiresAt"),
                    string(fields, "clientId"),
                    string(fields, "ticketGrantingServerId"),
                    string(fields, "clientTgsSessionKey"),
                    cryptoEnvelope(map(fields, "ticketTgsEnvelope")));
        } else if (type == SecureTgsRequest.class) {
            decoded = new SecureTgsRequest(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    string(fields, "ticketGrantingServerId"),
                    cryptoEnvelope(map(fields, "ticketTgsEnvelope")),
                    cryptoEnvelope(map(fields, "clientAuthenticatorEnvelope")));
        } else if (type == SecureTgsResponse.class) {
            decoded = new SecureTgsResponse(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    instant(fields, "expiresAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    string(fields, "clientServiceSessionKey"),
                    cryptoEnvelope(map(fields, "ticketServiceEnvelope")));
        } else if (type == SecureServiceRequest.class) {
            decoded = new SecureServiceRequest(
                    string(fields, "version"),
                    string(fields, "requestId"),
                    instant(fields, "issuedAt"),
                    string(fields, "clientId"),
                    string(fields, "serviceId"),
                    cryptoEnvelope(map(fields, "ticketServiceEnvelope")),
                    cryptoEnvelope(map(fields, "clientAuthenticatorEnvelope")));
        } else {
            throw new IllegalArgumentException("Tipo de payload no soportado: " + type.getName());
        }

        return type.cast(decoded);
    }

    private static Map<String, Object> payloadToMap(Object payload) {
        Objects.requireNonNull(payload, "payload");
        Map<String, Object> fields = new LinkedHashMap<>();

        if (payload instanceof AsRequest value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("clientId", value.clientId());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
        } else if (payload instanceof AsResponse value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
            fields.put("clientTgsSessionKey", value.clientTgsSessionKey());
            fields.put("ticketTgs", payloadToMap(value.ticketTgs()));
        } else if (payload instanceof TgsRequest value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
            fields.put("ticketTgs", nullableMap(value.ticketTgs()));
            fields.put("clientAuthenticator", nullableMap(value.clientAuthenticator()));
        } else if (payload instanceof TgsResponse value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("clientServiceSessionKey", value.clientServiceSessionKey());
            fields.put("ticketService", payloadToMap(value.ticketService()));
        } else if (payload instanceof ServiceRequest value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("ticketService", nullableMap(value.ticketService()));
            fields.put("clientAuthenticator", nullableMap(value.clientAuthenticator()));
        } else if (payload instanceof ServiceResponse value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("clientAuthenticatorIssuedAt", value.clientAuthenticatorIssuedAt().toString());
            fields.put("serverValidatedAt", value.serverValidatedAt().toString());
            fields.put("serviceMessage", value.serviceMessage());
            fields.put("accessGranted", value.accessGranted());
        } else if (payload instanceof TicketTgs value) {
            putTicket(fields, value.version(), value.ticketId(), value.issuedAt(), value.expiresAt(), value.clientId(),
                    value.clientAddress());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
            fields.put("clientTgsSessionKey", value.clientTgsSessionKey());
        } else if (payload instanceof TicketService value) {
            putTicket(fields, value.version(), value.ticketId(), value.issuedAt(), value.expiresAt(), value.clientId(),
                    value.clientAddress());
            fields.put("serviceId", value.serviceId());
            fields.put("clientServiceSessionKey", value.clientServiceSessionKey());
        } else if (payload instanceof ClientAuthenticator value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("clientAddress", value.clientAddress());
        } else if (payload instanceof ErrorResponse value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("source", value.source());
            fields.put("errorCode", value.errorCode());
            fields.put("errorMessage", value.errorMessage());
            fields.put("failedRequestId", value.failedRequestId());
        } else if (payload instanceof CryptoEnvelope value) {
            fields.put("version", value.version());
            fields.put("algorithm", value.algorithm());
            fields.put("iv", Base64.getEncoder().encodeToString(value.iv()));
            fields.put("ciphertext", Base64.getEncoder().encodeToString(value.ciphertext()));
            fields.put("issuedAt", value.issuedAt().toString());
        } else if (payload instanceof SecureAsResponse value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
            fields.put("clientTgsSessionKey", value.clientTgsSessionKey());
            fields.put("ticketTgsEnvelope", payloadToMap(value.ticketTgsEnvelope()));
        } else if (payload instanceof SecureTgsRequest value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("ticketGrantingServerId", value.ticketGrantingServerId());
            fields.put("ticketTgsEnvelope", payloadToMap(value.ticketTgsEnvelope()));
            fields.put("clientAuthenticatorEnvelope", payloadToMap(value.clientAuthenticatorEnvelope()));
        } else if (payload instanceof SecureTgsResponse value) {
            putExpiringMessage(fields, value.version(), value.requestId(), value.issuedAt(), value.expiresAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("clientServiceSessionKey", value.clientServiceSessionKey());
            fields.put("ticketServiceEnvelope", payloadToMap(value.ticketServiceEnvelope()));
        } else if (payload instanceof SecureServiceRequest value) {
            putMessage(fields, value.version(), value.requestId(), value.issuedAt());
            fields.put("clientId", value.clientId());
            fields.put("serviceId", value.serviceId());
            fields.put("ticketServiceEnvelope", payloadToMap(value.ticketServiceEnvelope()));
            fields.put("clientAuthenticatorEnvelope", payloadToMap(value.clientAuthenticatorEnvelope()));
        } else {
            throw new IllegalArgumentException("Payload no soportado: " + payload.getClass().getName());
        }

        return fields;
    }

    private static void putMessage(Map<String, Object> fields, String version, String requestId, Instant issuedAt) {
        fields.put("version", version);
        fields.put("requestId", requestId);
        fields.put("issuedAt", issuedAt.toString());
    }

    private static void putExpiringMessage(
            Map<String, Object> fields,
            String version,
            String requestId,
            Instant issuedAt,
            Instant expiresAt) {
        putMessage(fields, version, requestId, issuedAt);
        fields.put("expiresAt", expiresAt.toString());
    }

    private static void putTicket(
            Map<String, Object> fields,
            String version,
            String ticketId,
            Instant issuedAt,
            Instant expiresAt,
            String clientId,
            String clientAddress) {
        fields.put("version", version);
        fields.put("ticketId", ticketId);
        fields.put("issuedAt", issuedAt.toString());
        fields.put("expiresAt", expiresAt.toString());
        fields.put("clientId", clientId);
        fields.put("clientAddress", clientAddress);
    }

    private static Map<String, Object> nullableMap(Object payload) {
        return payload == null ? null : payloadToMap(payload);
    }

    private static TicketTgs ticketTgs(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        return new TicketTgs(
                string(fields, "version"),
                string(fields, "ticketId"),
                instant(fields, "issuedAt"),
                instant(fields, "expiresAt"),
                string(fields, "clientId"),
                string(fields, "clientAddress"),
                string(fields, "ticketGrantingServerId"),
                string(fields, "clientTgsSessionKey"));
    }

    private static TicketService ticketService(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        return new TicketService(
                string(fields, "version"),
                string(fields, "ticketId"),
                instant(fields, "issuedAt"),
                instant(fields, "expiresAt"),
                string(fields, "clientId"),
                string(fields, "clientAddress"),
                string(fields, "serviceId"),
                string(fields, "clientServiceSessionKey"));
    }

    private static ClientAuthenticator authenticator(Map<String, Object> fields) {
        if (fields == null) {
            return null;
        }
        return new ClientAuthenticator(
                string(fields, "version"),
                string(fields, "requestId"),
                instant(fields, "issuedAt"),
                instant(fields, "expiresAt"),
                string(fields, "clientId"),
                string(fields, "clientAddress"));
    }

    private static CryptoEnvelope cryptoEnvelope(Map<String, Object> fields) {
        return new CryptoEnvelope(
                string(fields, "version"),
                string(fields, "algorithm"),
                Base64.getDecoder().decode(string(fields, "iv")),
                Base64.getDecoder().decode(string(fields, "ciphertext")),
                instant(fields, "issuedAt"));
    }

    private static String withoutTrailingObject(Map<String, Object> fields) {
        String json = toJsonValue(fields);
        return json.substring(0, json.length() - 1);
    }

    private static String toJsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return quote(string);
        }
        if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> map) {
            StringBuilder json = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append(quote(entry.getKey().toString()));
                json.append(':');
                json.append(toJsonValue(entry.getValue()));
            }
            return json.append('}').toString();
        }
        return toJsonValue(payloadToMap(value));
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) ch));
                    } else {
                        escaped.append(ch);
                    }
                }
            }
        }
        return escaped.append('"').toString();
    }

    private static Map<String, Object> object(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON vacio");
        }
        Object value = new JsonReader(json).parse();
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                typed.put(entry.getKey().toString(), entry.getValue());
            }
            return typed;
        }
        throw new IllegalArgumentException("Se esperaba un objeto JSON");
    }

    private static Map<String, Object> map(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                typed.put(entry.getKey().toString(), entry.getValue());
            }
            return typed;
        }
        throw new IllegalArgumentException("Falta objeto JSON " + key);
    }

    private static Map<String, Object> mapOrNull(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        return map(fields, key);
    }

    private static String string(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Falta string JSON " + key);
    }

    private static Instant instant(Map<String, Object> fields, String key) {
        return Instant.parse(string(fields, key));
    }

    private static boolean bool(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        throw new IllegalArgumentException("Falta boolean JSON " + key);
    }

    private static MessageType messageType(Map<String, Object> fields) {
        String typeName = string(fields, "messageType");
        try {
            return MessageType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("MessageType no soportado: " + typeName, e);
        }
    }

    private static final class JsonReader {
        private final String json;
        private int position;

        private JsonReader(String json) {
            this.json = Objects.requireNonNull(json, "json");
        }

        private Object parse() {
            Object value = parseValue();
            skipWhitespace();
            if (position != json.length()) {
                throw new IllegalArgumentException("JSON con contenido extra en posicion " + position);
            }
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (position >= json.length()) {
                throw new IllegalArgumentException("JSON incompleto");
            }

            char ch = json.charAt(position);
            return switch (ch) {
                case '{' -> parseObject();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> object = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                position++;
                return object;
            }

            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                object.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    position++;
                    return object;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < json.length()) {
                char ch = json.charAt(position++);
                if (ch == '"') {
                    return value.toString();
                }
                if (ch != '\\') {
                    value.append(ch);
                    continue;
                }
                if (position >= json.length()) {
                    throw new IllegalArgumentException("Escape JSON incompleto");
                }
                char escaped = json.charAt(position++);
                switch (escaped) {
                    case '"' -> value.append('"');
                    case '\\' -> value.append('\\');
                    case '/' -> value.append('/');
                    case 'b' -> value.append('\b');
                    case 'f' -> value.append('\f');
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case 'u' -> {
                        if (position + 4 > json.length()) {
                            throw new IllegalArgumentException("Escape unicode incompleto");
                        }
                        String hex = json.substring(position, position + 4);
                        try {
                            value.append((char) Integer.parseInt(hex, 16));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Escape unicode invalido", e);
                        }
                        position += 4;
                    }
                    default -> throw new IllegalArgumentException("Escape JSON invalido: " + escaped);
                }
            }
            throw new IllegalArgumentException("String JSON sin cierre");
        }

        private Object parseLiteral(String literal, Object value) {
            if (!json.startsWith(literal, position)) {
                throw new IllegalArgumentException("Literal JSON invalido en posicion " + position);
            }
            position += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = position;
            while (position < json.length()) {
                char ch = json.charAt(position);
                if ((ch >= '0' && ch <= '9') || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                    position++;
                } else {
                    break;
                }
            }
            if (start == position) {
                throw new IllegalArgumentException("Valor JSON invalido en posicion " + position);
            }
            String number = json.substring(start, position);
            try {
                return number.contains(".") || number.contains("e") || number.contains("E")
                        ? Double.parseDouble(number)
                        : Long.parseLong(number);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Numero JSON invalido en posicion " + start, e);
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (position >= json.length() || json.charAt(position) != expected) {
                throw new IllegalArgumentException("Se esperaba '" + expected + "' en posicion " + position);
            }
            position++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return position < json.length() && json.charAt(position) == expected;
        }

        private void skipWhitespace() {
            while (position < json.length()) {
                char ch = json.charAt(position);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    position++;
                } else {
                    return;
                }
            }
        }
    }
}
