package com.portfolio.auth.gateway;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class WebSocketMessageCodec {

    public String encode(WebSocketMessage message) {
        Objects.requireNonNull(message, "message");
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("type", message.type().name());
        putIfPresent(fields, "requestId", message.requestId());
        putIfPresent(fields, "clientId", message.clientId());
        putIfPresent(fields, "serviceId", message.serviceId());
        putIfPresent(fields, "stage", message.stage());
        putIfPresent(fields, "message", message.message());
        putIfPresent(fields, "success", message.success());
        putIfPresent(fields, "serviceMessage", message.serviceMessage());
        putIfPresent(fields, "asMillis", message.asMillis());
        putIfPresent(fields, "tgsMillis", message.tgsMillis());
        putIfPresent(fields, "serviceMillis", message.serviceMillis());
        putIfPresent(fields, "totalMillis", message.totalMillis());
        return toJsonObject(fields);
    }

    public WebSocketMessage decode(String json) {
        Map<String, Object> fields = object(json);
        WebSocketMessageType type = type(fields);
        return WebSocketMessage.inbound(
                type,
                stringOrNull(fields, "requestId"),
                stringOrNull(fields, "clientId"),
                stringOrNull(fields, "serviceId"));
    }

    private static void putIfPresent(Map<String, Object> fields, String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }
    }

    private static WebSocketMessageType type(Map<String, Object> fields) {
        String typeName = string(fields, "type");
        try {
            return WebSocketMessageType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("WebSocketMessageType no soportado: " + typeName, e);
        }
    }

    private static String string(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Falta string JSON " + key);
    }

    private static String stringOrNull(Map<String, Object> fields, String key) {
        Object value = fields.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("Campo JSON invalido " + key);
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

    private static String toJsonObject(Map<String, Object> fields) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey()));
            json.append(':');
            json.append(toJsonValue(entry.getValue()));
        }
        return json.append('}').toString();
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
        throw new IllegalArgumentException("Valor JSON no soportado: " + value.getClass().getName());
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
