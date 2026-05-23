package com.portfolio.auth.client.audit;

import com.portfolio.auth.client.AuthClient;
import com.portfolio.auth.core.config.AuthConfig;
import com.portfolio.auth.core.protocol.dto.ServiceResponse;
import com.portfolio.auth.transport.secure.SecureAsResponse;
import com.portfolio.auth.transport.secure.SecureTgsResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class ModularAuthAuditRunner {
    private ModularAuthAuditRunner() {
    }

    public static void main(String[] args) throws Exception {
        int iterations = iterations(args);
        Path outputDirectory = outputDirectory();
        AuthConfig config = AuthConfig.fromEnvironment();
        AuthClient client = new AuthClient(config);
        Instant startedAt = Instant.now();
        long auditStarted = System.nanoTime();
        List<IterationResult> results = new ArrayList<>();

        for (int index = 1; index <= iterations; index++) {
            results.add(runIteration(client, config, index));
        }

        long totalNanos = System.nanoTime() - auditStarted;
        AuditReport report = new AuditReport(
                startedAt,
                iterations,
                results,
                totalNanos,
                config,
                System.getProperty("java.version", "unknown"),
                System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", ""),
                commitHash(),
                System.getProperty("sun.java.command", ModularAuthAuditRunner.class.getName()));

        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("latest-run.md"), report.toMarkdown(), StandardCharsets.UTF_8);
        Files.writeString(outputDirectory.resolve("latest-run.json"), report.toJson(), StandardCharsets.UTF_8);

        System.out.println("Audit written to " + outputDirectory.toAbsolutePath());
        System.out.println("successes=" + report.successes() + " failures=" + report.failures()
                + " avgTotalMs=" + formatMillis(report.totalSummary().averageMillis()));

        if (report.failures() > 0) {
            System.exit(1);
        }
    }

    private static IterationResult runIteration(AuthClient client, AuthConfig config, int index) {
        String runId = UUID.randomUUID().toString();
        long iterationStarted = System.nanoTime();
        long asNanos = -1;
        long tgsNanos = -1;
        long serviceNanos = -1;
        try {
            long stageStarted = System.nanoTime();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket("audit-as-" + index + "-" + runId);
            asNanos = System.nanoTime() - stageStarted;

            stageStarted = System.nanoTime();
            SecureTgsResponse tgsResponse = client.requestServiceTicket(
                    asResponse,
                    config.defaultServiceId(),
                    "audit-tgs-" + index + "-" + runId,
                    "audit-auth-tgs-" + index + "-" + runId,
                    Instant.now());
            tgsNanos = System.nanoTime() - stageStarted;

            stageStarted = System.nanoTime();
            ServiceResponse serviceResponse = client.requestProtectedService(
                    tgsResponse,
                    "audit-service-" + index + "-" + runId,
                    "audit-auth-service-" + index + "-" + runId,
                    Instant.now());
            serviceNanos = System.nanoTime() - stageStarted;

            boolean success = serviceResponse.accessGranted();
            return new IterationResult(
                    index,
                    success,
                    asNanos,
                    tgsNanos,
                    serviceNanos,
                    System.nanoTime() - iterationStarted,
                    success ? "" : "SERVICE_DENIED");
        } catch (Exception e) {
            return new IterationResult(
                    index,
                    false,
                    asNanos,
                    tgsNanos,
                    serviceNanos,
                    System.nanoTime() - iterationStarted,
                    e.getClass().getSimpleName() + ": " + safeMessage(e));
        }
    }

    private static int iterations(String[] args) {
        String configured = System.getenv("AUTH_AUDIT_ITERATIONS");
        int iterations = configured == null || configured.isBlank() ? 5 : parsePositive(configured);
        for (int index = 0; index < args.length; index++) {
            if ("--iterations".equals(args[index]) && index + 1 < args.length) {
                iterations = parsePositive(args[index + 1]);
                index++;
            } else if (args[index].matches("\\d+")) {
                iterations = parsePositive(args[index]);
            }
        }
        return iterations;
    }

    private static int parsePositive(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("iterations debe ser positivo");
        }
        return parsed;
    }

    private static Path outputDirectory() {
        String configured = System.getenv("AUTH_AUDIT_OUTPUT_DIR");
        if (configured == null || configured.isBlank()) {
            return Path.of("docs", "audits");
        }
        return Path.of(configured);
    }

    private static String commitHash() {
        ProcessBuilder builder = new ProcessBuilder("git", "rev-parse", "--short", "HEAD");
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String value;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                value = reader.readLine();
            }
            if (process.waitFor() == 0 && value != null && !value.isBlank()) {
                return value.trim();
            }
        } catch (IOException e) {
            return "unavailable: " + e.getClass().getSimpleName();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "unavailable: interrupted";
        }
        return "unavailable";
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    private static String formatMillis(double nanos) {
        return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0);
    }

    private record IterationResult(
            int index,
            boolean success,
            long asNanos,
            long tgsNanos,
            long serviceNanos,
            long totalNanos,
            String error) {
    }

    private record StageSummary(double minMillis, double maxMillis, double averageMillis) {
        static StageSummary from(List<Long> values) {
            DoubleSummaryStatistics stats = values.stream()
                    .filter(value -> value >= 0)
                    .mapToDouble(Long::doubleValue)
                    .summaryStatistics();
            if (stats.getCount() == 0) {
                return new StageSummary(0, 0, 0);
            }
            return new StageSummary(stats.getMin(), stats.getMax(), stats.getAverage());
        }
    }

    private record AuditReport(
            Instant startedAt,
            int iterations,
            List<IterationResult> results,
            long elapsedNanos,
            AuthConfig config,
            String javaVersion,
            String operatingSystem,
            String commitHash,
            String command) {

        private AuditReport {
            Objects.requireNonNull(results, "results");
        }

        int successes() {
            return (int) results.stream().filter(IterationResult::success).count();
        }

        int failures() {
            return iterations - successes();
        }

        double throughputPerSecond() {
            if (elapsedNanos <= 0) {
                return 0;
            }
            return successes() / (elapsedNanos / 1_000_000_000.0);
        }

        StageSummary asSummary() {
            return StageSummary.from(results.stream().map(IterationResult::asNanos).toList());
        }

        StageSummary tgsSummary() {
            return StageSummary.from(results.stream().map(IterationResult::tgsNanos).toList());
        }

        StageSummary serviceSummary() {
            return StageSummary.from(results.stream().map(IterationResult::serviceNanos).toList());
        }

        StageSummary totalSummary() {
            return StageSummary.from(results.stream().map(IterationResult::totalNanos).toList());
        }

        String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Modular Auth Audit\n\n");
            markdown.append("- Fecha/hora: `").append(startedAt).append("`\n");
            markdown.append("- Java: `").append(javaVersion).append("`\n");
            markdown.append("- Sistema operativo: `").append(operatingSystem).append("`\n");
            markdown.append("- Commit: `").append(commitHash).append("`\n");
            markdown.append("- Comando: `").append(command).append("`\n");
            markdown.append("- Puertos: AS `").append(config.authenticationServerPort())
                    .append("`, TGS `").append(config.ticketGrantingServerPort())
                    .append("`, Service `").append(config.serviceServerPort()).append("`\n");
            markdown.append("- Iteraciones: `").append(iterations).append("`\n");
            markdown.append("- Exitos: `").append(successes()).append("`\n");
            markdown.append("- Fallos: `").append(failures()).append("`\n");
            markdown.append("- Throughput aproximado: `")
                    .append(String.format(Locale.ROOT, "%.3f", throughputPerSecond()))
                    .append(" flujos/s`\n\n");
            markdown.append("| Etapa | Min ms | Max ms | Promedio ms |\n");
            markdown.append("| --- | ---: | ---: | ---: |\n");
            appendStage(markdown, "AS exchange", asSummary());
            appendStage(markdown, "TGS exchange", tgsSummary());
            appendStage(markdown, "Service exchange", serviceSummary());
            appendStage(markdown, "Total", totalSummary());
            markdown.append("\n| Iteracion | Estado | AS ms | TGS ms | Service ms | Total ms | Error |\n");
            markdown.append("| ---: | --- | ---: | ---: | ---: | ---: | --- |\n");
            for (IterationResult result : results) {
                markdown.append("| ").append(result.index())
                        .append(" | ").append(result.success() ? "OK" : "FAIL")
                        .append(" | ").append(formatMillis(result.asNanos()))
                        .append(" | ").append(formatMillis(result.tgsNanos()))
                        .append(" | ").append(formatMillis(result.serviceNanos()))
                        .append(" | ").append(formatMillis(result.totalNanos()))
                        .append(" | ").append(result.error().isBlank() ? "" : escapeMarkdown(result.error()))
                        .append(" |\n");
            }
            return markdown.toString();
        }

        String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            appendJsonField(json, "startedAt", startedAt.toString(), true);
            appendJsonField(json, "javaVersion", javaVersion, true);
            appendJsonField(json, "operatingSystem", operatingSystem, true);
            appendJsonField(json, "commitHash", commitHash, true);
            appendJsonField(json, "command", command, true);
            json.append("  \"ports\": {\"as\": ").append(config.authenticationServerPort())
                    .append(", \"tgs\": ").append(config.ticketGrantingServerPort())
                    .append(", \"service\": ").append(config.serviceServerPort()).append("},\n");
            json.append("  \"iterations\": ").append(iterations).append(",\n");
            json.append("  \"successes\": ").append(successes()).append(",\n");
            json.append("  \"failures\": ").append(failures()).append(",\n");
            json.append("  \"throughputPerSecond\": ")
                    .append(String.format(Locale.ROOT, "%.6f", throughputPerSecond())).append(",\n");
            json.append("  \"latencyMs\": {\n");
            appendStageJson(json, "asExchange", asSummary(), true);
            appendStageJson(json, "tgsExchange", tgsSummary(), true);
            appendStageJson(json, "serviceExchange", serviceSummary(), true);
            appendStageJson(json, "total", totalSummary(), false);
            json.append("  },\n");
            json.append("  \"results\": [\n");
            for (int index = 0; index < results.size(); index++) {
                IterationResult result = results.get(index);
                json.append("    {\"index\": ").append(result.index())
                        .append(", \"success\": ").append(result.success())
                        .append(", \"asMs\": ").append(formatMillis(result.asNanos()))
                        .append(", \"tgsMs\": ").append(formatMillis(result.tgsNanos()))
                        .append(", \"serviceMs\": ").append(formatMillis(result.serviceNanos()))
                        .append(", \"totalMs\": ").append(formatMillis(result.totalNanos()))
                        .append(", \"error\": \"").append(escapeJson(result.error())).append("\"}");
                if (index + 1 < results.size()) {
                    json.append(',');
                }
                json.append('\n');
            }
            json.append("  ]\n");
            json.append("}\n");
            return json.toString();
        }

        private static void appendStage(StringBuilder markdown, String name, StageSummary summary) {
            markdown.append("| ").append(name)
                    .append(" | ").append(formatMillis(summary.minMillis()))
                    .append(" | ").append(formatMillis(summary.maxMillis()))
                    .append(" | ").append(formatMillis(summary.averageMillis()))
                    .append(" |\n");
        }

        private static void appendJsonField(StringBuilder json, String key, String value, boolean comma) {
            json.append("  \"").append(key).append("\": \"").append(escapeJson(value)).append("\"");
            json.append(comma ? ",\n" : "\n");
        }

        private static void appendStageJson(
                StringBuilder json,
                String key,
                StageSummary summary,
                boolean comma) {
            json.append("    \"").append(key).append("\": {\"min\": ")
                    .append(formatMillis(summary.minMillis()))
                    .append(", \"max\": ").append(formatMillis(summary.maxMillis()))
                    .append(", \"average\": ").append(formatMillis(summary.averageMillis()))
                    .append('}');
            json.append(comma ? ",\n" : "\n");
        }

        private static String escapeJson(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }

        private static String escapeMarkdown(String value) {
            return value.replace("|", "\\|");
        }
    }
}
