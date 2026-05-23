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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class ModularAuthConcurrencyAuditRunner {
    public static final int DEFAULT_CONCURRENT_CLIENTS = 25;
    public static final int DEFAULT_TOTAL_FLOWS = 100;

    private ModularAuthConcurrencyAuditRunner() {
    }

    public static void main(String[] args) throws Exception {
        int concurrentClients = intArg(args, "--clients", "AUTH_CONCURRENCY_CLIENTS", DEFAULT_CONCURRENT_CLIENTS);
        int totalFlows = intArg(args, "--flows", "AUTH_CONCURRENCY_FLOWS", DEFAULT_TOTAL_FLOWS);
        Path outputDirectory = outputDirectory();
        AuthConfig config = AuthConfig.fromEnvironment();

        ConcurrencyReport report = run(
                () -> new AuthClient(config),
                config,
                concurrentClients,
                totalFlows,
                System.getProperty("sun.java.command", ModularAuthConcurrencyAuditRunner.class.getName()));
        writeReport(outputDirectory, report);

        System.out.println("Concurrency audit written to " + outputDirectory.toAbsolutePath());
        System.out.println("successes=" + report.successes() + " failures=" + report.failures()
                + " avgTotalMs=" + formatMillis(report.totalSummary().averageMillis())
                + " p95TotalMs=" + formatMillis(report.totalP95Millis()));

        if (report.failures() > 0) {
            System.exit(1);
        }
    }

    public static ConcurrencyReport run(
            Supplier<AuthClient> clientFactory,
            AuthConfig config,
            int concurrentClients,
            int totalFlows,
            String command) throws Exception {
        Objects.requireNonNull(clientFactory, "clientFactory");
        Objects.requireNonNull(config, "config");
        if (concurrentClients <= 0) {
            throw new IllegalArgumentException("concurrentClients debe ser positivo");
        }
        if (totalFlows <= 0) {
            throw new IllegalArgumentException("totalFlows debe ser positivo");
        }

        Instant startedAt = Instant.now();
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentClients);
        List<Future<FlowResult>> futures = new ArrayList<>();
        long auditStarted = System.nanoTime();
        try {
            for (int index = 1; index <= totalFlows; index++) {
                int flowIndex = index;
                futures.add(executor.submit(flowTask(clientFactory, config, startGate, flowIndex)));
            }
            startGate.countDown();

            List<FlowResult> results = new ArrayList<>();
            for (Future<FlowResult> future : futures) {
                results.add(future.get());
            }

            long elapsedNanos = System.nanoTime() - auditStarted;
            results.sort(Comparator.comparingInt(FlowResult::index));
            return new ConcurrencyReport(
                    startedAt,
                    concurrentClients,
                    totalFlows,
                    elapsedNanos,
                    config,
                    System.getProperty("java.version", "unknown"),
                    System.getProperty("os.name", "unknown") + " " + System.getProperty("os.version", ""),
                    commitHash(),
                    command,
                    results);
        } finally {
            executor.shutdownNow();
        }
    }

    public static void writeReport(Path outputDirectory, ConcurrencyReport report) throws IOException {
        Files.createDirectories(outputDirectory);
        Files.writeString(
                outputDirectory.resolve("concurrency-latest-run.md"),
                report.toMarkdown(),
                StandardCharsets.UTF_8);
        Files.writeString(
                outputDirectory.resolve("concurrency-latest-run.json"),
                report.toJson(),
                StandardCharsets.UTF_8);
    }

    private static Callable<FlowResult> flowTask(
            Supplier<AuthClient> clientFactory,
            AuthConfig config,
            CountDownLatch startGate,
            int flowIndex) {
        return () -> {
            startGate.await();
            return runFlow(clientFactory.get(), config, flowIndex);
        };
    }

    private static FlowResult runFlow(AuthClient client, AuthConfig config, int index) {
        String runId = UUID.randomUUID().toString();
        long flowStarted = System.nanoTime();
        long asNanos = -1;
        long tgsNanos = -1;
        long serviceNanos = -1;
        try {
            long stageStarted = System.nanoTime();
            SecureAsResponse asResponse = client.requestTicketGrantingTicket("concurrency-as-" + index + "-" + runId);
            asNanos = System.nanoTime() - stageStarted;

            stageStarted = System.nanoTime();
            SecureTgsResponse tgsResponse = client.requestServiceTicket(
                    asResponse,
                    config.defaultServiceId(),
                    "concurrency-tgs-" + index + "-" + runId,
                    "concurrency-auth-tgs-" + index + "-" + runId,
                    Instant.now());
            tgsNanos = System.nanoTime() - stageStarted;

            stageStarted = System.nanoTime();
            ServiceResponse serviceResponse = client.requestProtectedService(
                    tgsResponse,
                    "concurrency-service-" + index + "-" + runId,
                    "concurrency-auth-service-" + index + "-" + runId,
                    Instant.now());
            serviceNanos = System.nanoTime() - stageStarted;

            boolean success = serviceResponse.accessGranted();
            return new FlowResult(
                    index,
                    success,
                    asNanos,
                    tgsNanos,
                    serviceNanos,
                    System.nanoTime() - flowStarted,
                    "",
                    success ? "" : "SERVICE_DENIED");
        } catch (Exception e) {
            return new FlowResult(
                    index,
                    false,
                    asNanos,
                    tgsNanos,
                    serviceNanos,
                    System.nanoTime() - flowStarted,
                    e.getClass().getSimpleName(),
                    safeMessage(e));
        }
    }

    private static int intArg(String[] args, String flag, String envKey, int defaultValue) {
        String configured = System.getenv(envKey);
        int value = configured == null || configured.isBlank() ? defaultValue : parsePositive(configured);
        for (int index = 0; index < args.length; index++) {
            if (flag.equals(args[index]) && index + 1 < args.length) {
                value = parsePositive(args[index + 1]);
                index++;
            }
        }
        return value;
    }

    private static int parsePositive(String value) {
        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("El valor debe ser positivo");
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

    public record FlowResult(
            int index,
            boolean success,
            long asNanos,
            long tgsNanos,
            long serviceNanos,
            long totalNanos,
            String errorType,
            String errorMessage) {
    }

    public record StageSummary(double minMillis, double maxMillis, double averageMillis) {
        static StageSummary from(List<Long> values) {
            List<Long> valid = values.stream()
                    .filter(value -> value >= 0)
                    .toList();
            if (valid.isEmpty()) {
                return new StageSummary(0, 0, 0);
            }
            double min = valid.stream().mapToLong(Long::longValue).min().orElse(0);
            double max = valid.stream().mapToLong(Long::longValue).max().orElse(0);
            double average = valid.stream().mapToDouble(Long::doubleValue).average().orElse(0);
            return new StageSummary(min, max, average);
        }
    }

    public record ConcurrencyReport(
            Instant startedAt,
            int concurrentClients,
            int totalFlows,
            long elapsedNanos,
            AuthConfig config,
            String javaVersion,
            String operatingSystem,
            String commitHash,
            String command,
            List<FlowResult> results) {

        public ConcurrencyReport {
            Objects.requireNonNull(results, "results");
        }

        public int successes() {
            return (int) results.stream().filter(FlowResult::success).count();
        }

        public int failures() {
            return totalFlows - successes();
        }

        public double throughputPerSecond() {
            if (elapsedNanos <= 0) {
                return 0;
            }
            return successes() / (elapsedNanos / 1_000_000_000.0);
        }

        public StageSummary totalSummary() {
            return StageSummary.from(results.stream().map(FlowResult::totalNanos).toList());
        }

        public double totalP95Millis() {
            List<Long> sorted = results.stream()
                    .map(FlowResult::totalNanos)
                    .filter(value -> value >= 0)
                    .sorted()
                    .toList();
            if (sorted.isEmpty()) {
                return 0;
            }
            int index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1);
            return sorted.get(index);
        }

        private StageSummary asSummary() {
            return StageSummary.from(results.stream().map(FlowResult::asNanos).toList());
        }

        private StageSummary tgsSummary() {
            return StageSummary.from(results.stream().map(FlowResult::tgsNanos).toList());
        }

        private StageSummary serviceSummary() {
            return StageSummary.from(results.stream().map(FlowResult::serviceNanos).toList());
        }

        private Map<String, Long> errorsByType() {
            return results.stream()
                    .filter(result -> !result.success())
                    .collect(Collectors.groupingBy(
                            result -> result.errorType().isBlank() ? "UNKNOWN" : result.errorType(),
                            LinkedHashMap::new,
                            Collectors.counting()));
        }

        public String toMarkdown() {
            StringBuilder markdown = new StringBuilder();
            markdown.append("# Modular Auth Concurrency Audit\n\n");
            markdown.append("- Fecha/hora: `").append(startedAt).append("`\n");
            markdown.append("- Java: `").append(javaVersion).append("`\n");
            markdown.append("- Sistema operativo: `").append(operatingSystem).append("`\n");
            markdown.append("- Commit: `").append(commitHash).append("`\n");
            markdown.append("- Comando: `").append(command).append("`\n");
            markdown.append("- Clientes concurrentes: `").append(concurrentClients).append("`\n");
            markdown.append("- Flujos totales: `").append(totalFlows).append("`\n");
            markdown.append("- Exitos: `").append(successes()).append("`\n");
            markdown.append("- Fallos: `").append(failures()).append("`\n");
            markdown.append("- Throughput aproximado: `")
                    .append(String.format(Locale.ROOT, "%.3f", throughputPerSecond()))
                    .append(" flujos/s`\n");
            markdown.append("- P95 total: `").append(formatMillis(totalP95Millis())).append(" ms`\n\n");
            markdown.append("| Etapa | Min ms | Max ms | Promedio ms |\n");
            markdown.append("| --- | ---: | ---: | ---: |\n");
            appendStage(markdown, "AS exchange", asSummary());
            appendStage(markdown, "TGS exchange", tgsSummary());
            appendStage(markdown, "Service exchange", serviceSummary());
            appendStage(markdown, "Total", totalSummary());
            markdown.append("\n## Errores Por Tipo\n\n");
            if (errorsByType().isEmpty()) {
                markdown.append("Sin errores.\n");
            } else {
                errorsByType().forEach((type, count) -> markdown.append("- `")
                        .append(type)
                        .append("`: `")
                        .append(count)
                        .append("`\n"));
            }
            return markdown.toString();
        }

        public String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            appendJsonField(json, "startedAt", startedAt.toString(), true);
            appendJsonField(json, "javaVersion", javaVersion, true);
            appendJsonField(json, "operatingSystem", operatingSystem, true);
            appendJsonField(json, "commitHash", commitHash, true);
            appendJsonField(json, "command", command, true);
            json.append("  \"concurrentClients\": ").append(concurrentClients).append(",\n");
            json.append("  \"totalFlows\": ").append(totalFlows).append(",\n");
            json.append("  \"successes\": ").append(successes()).append(",\n");
            json.append("  \"failures\": ").append(failures()).append(",\n");
            json.append("  \"throughputPerSecond\": ")
                    .append(String.format(Locale.ROOT, "%.6f", throughputPerSecond())).append(",\n");
            json.append("  \"p95TotalMs\": ").append(formatMillis(totalP95Millis())).append(",\n");
            json.append("  \"latencyMs\": {\n");
            appendStageJson(json, "asExchange", asSummary(), true);
            appendStageJson(json, "tgsExchange", tgsSummary(), true);
            appendStageJson(json, "serviceExchange", serviceSummary(), true);
            appendStageJson(json, "total", totalSummary(), false);
            json.append("  },\n");
            json.append("  \"errorsByType\": {");
            int index = 0;
            for (Map.Entry<String, Long> entry : errorsByType().entrySet()) {
                if (index++ > 0) {
                    json.append(", ");
                }
                json.append("\"").append(escapeJson(entry.getKey())).append("\": ").append(entry.getValue());
            }
            json.append("}\n");
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
    }
}
