package com.portfolio.auth.core.observability;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StructuredLog {
    private StructuredLog() {
    }

    public static void info(String service, String event, String status, RequestContext context, long latencyMs) {
        write("INFO", service, event, status, context, latencyMs, null);
    }

    public static void warn(String service, String event, String status, RequestContext context, long latencyMs,
            String errorType) {
        write("WARN", service, event, status, context, latencyMs, errorType);
    }

    public static void error(String service, String event, String status, RequestContext context, long latencyMs,
            String errorType) {
        write("ERROR", service, event, status, context, latencyMs, errorType);
    }

    private static void write(
            String level,
            String service,
            String event,
            String status,
            RequestContext context,
            long latencyMs,
            String errorType) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("timestamp", Instant.now());
        fields.put("level", level);
        fields.put("service", service);
        fields.put("requestId", context == null ? null : context.requestId());
        fields.put("clientId", context == null ? null : context.clientId());
        fields.put("serviceId", context == null ? null : context.serviceId());
        fields.put("event", event);
        fields.put("status", status);
        fields.put("latencyMs", latencyMs);
        fields.put("errorType", errorType);
        System.out.println(JsonLogFormatter.format(fields));
    }
}
