package com.portfolio.auth.core.observability;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public final class JsonLogFormatter {
    private JsonLogFormatter() {
    }

    public static String format(Map<String, ?> fields) {
        Objects.requireNonNull(fields, "fields");
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(quote(entry.getKey())).append(':').append(value(entry.getKey(), entry.getValue()));
        }
        return json.append('}').toString();
    }

    private static String value(String key, Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Instant instant) {
            return quote(instant.toString());
        }
        return quote(sanitized(key, value.toString()));
    }

    private static String sanitized(String key, String value) {
        String normalized = key.toLowerCase();
        if (normalized.contains("secret")
                || normalized.contains("password")
                || normalized.contains("ticket")
                || normalized.contains("ciphertext")
                || normalized.contains("sessionid")
                || normalized.contains("connection")) {
            return "<redacted>";
        }
        return value;
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
