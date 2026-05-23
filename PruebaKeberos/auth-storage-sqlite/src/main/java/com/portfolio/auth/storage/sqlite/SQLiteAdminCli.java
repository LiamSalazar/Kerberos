package com.portfolio.auth.storage.sqlite;

import com.portfolio.auth.core.audit.AuthAuditEvent;
import com.portfolio.auth.core.config.AuthConfig;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public final class SQLiteAdminCli {
    private SQLiteAdminCli() {
    }

    public static void main(String[] args) throws Exception {
        CliArgs cli = CliArgs.parse(args);
        if (cli.command().isEmpty() || cli.has("--help")) {
            printUsage();
            return;
        }

        Path database = cli.databasePath();
        SQLiteMigrationRunner.applyMigrations(database, cli.migrationsPath());
        SQLiteAdminRepository admin = new SQLiteAdminRepository(database);

        String area = cli.command().get(0);
        switch (area) {
            case "clients" -> handleClients(admin, cli.command().subList(1, cli.command().size()), cli);
            case "services" -> handleServices(admin, cli.command().subList(1, cli.command().size()), cli);
            case "audit" -> handleAudit(database, cli.command().subList(1, cli.command().size()), cli);
            default -> throw new IllegalArgumentException("Unknown admin area: " + area);
        }
    }

    private static void handleClients(SQLiteAdminRepository admin, List<String> command, CliArgs cli) {
        String action = first(command, "clients action");
        switch (action) {
            case "add", "register" -> {
                String id = cli.require("--id");
                admin.upsertClient(
                        id,
                        cli.require("--display-name"),
                        cli.require("--secret"),
                        !cli.has("--disabled"));
                System.out.println("Client registered: id=" + id + ", enabled=" + !cli.has("--disabled"));
            }
            case "list" -> {
                System.out.println("Clients:");
                for (SQLiteClientRecord client : admin.listClients()) {
                    System.out.println("- id=" + client.id()
                            + ", displayName=" + client.displayName()
                            + ", enabled=" + client.enabled());
                }
            }
            case "enable" -> printUpdated("Client", cli.require("--id"), admin.setClientEnabled(cli.require("--id"), true));
            case "disable" -> printUpdated("Client", cli.require("--id"), admin.setClientEnabled(cli.require("--id"), false));
            default -> throw new IllegalArgumentException("Unknown clients action: " + action);
        }
    }

    private static void handleServices(SQLiteAdminRepository admin, List<String> command, CliArgs cli) {
        String action = first(command, "services action");
        switch (action) {
            case "add", "register" -> {
                String id = cli.require("--id");
                admin.upsertService(
                        id,
                        cli.require("--display-name"),
                        cli.require("--secret"),
                        cli.value("--endpoint", "local://" + id),
                        !cli.has("--disabled"));
                System.out.println("Service registered: id=" + id + ", enabled=" + !cli.has("--disabled"));
            }
            case "list" -> {
                System.out.println("Services:");
                for (SQLiteServiceRecord service : admin.listServices()) {
                    System.out.println("- id=" + service.id()
                            + ", displayName=" + service.displayName()
                            + ", endpoint=" + service.endpoint()
                            + ", enabled=" + service.enabled());
                }
            }
            case "enable" -> printUpdated("Service", cli.require("--id"), admin.setServiceEnabled(cli.require("--id"), true));
            case "disable" -> printUpdated("Service", cli.require("--id"), admin.setServiceEnabled(cli.require("--id"), false));
            default -> throw new IllegalArgumentException("Unknown services action: " + action);
        }
    }

    private static void handleAudit(Path database, List<String> command, CliArgs cli) {
        String action = first(command, "audit action");
        SQLiteAuditRepository audit = new SQLiteAuditRepository(database);
        List<AuthAuditEvent> events = switch (action) {
            case "list" -> audit.findRecent(cli.intValue("--limit", 20));
            case "by-request" -> audit.findByRequestId(cli.require("--request-id"));
            case "by-client" -> audit.findByClientId(cli.require("--client-id"));
            case "by-service" -> audit.findByServiceId(cli.require("--service-id"));
            default -> throw new IllegalArgumentException("Unknown audit action: " + action);
        };
        System.out.println("Audit events:");
        for (AuthAuditEvent event : events) {
            System.out.println("- requestId=" + event.requestId()
                    + ", clientId=" + value(event.clientId())
                    + ", serviceId=" + value(event.serviceId())
                    + ", eventType=" + event.eventType()
                    + ", status=" + event.status()
                    + ", errorType=" + value(event.errorType())
                    + ", latencyMs=" + event.latencyMs()
                    + ", createdAt=" + event.createdAt());
        }
    }

    private static void printUpdated(String label, String id, boolean updated) {
        System.out.println(label + " " + (updated ? "updated" : "not found") + ": id=" + id);
    }

    private static String first(List<String> command, String label) {
        if (command.isEmpty()) {
            throw new IllegalArgumentException("Missing " + label);
        }
        return command.get(0);
    }

    private static String value(String value) {
        return value == null ? "-" : value;
    }

    private static void printUsage() {
        System.out.println("""
                Usage:
                  SQLiteAdminCli [--db <path>] clients add --id <id> --display-name <name> --secret <secret>
                  SQLiteAdminCli [--db <path>] clients list
                  SQLiteAdminCli [--db <path>] clients enable --id <id>
                  SQLiteAdminCli [--db <path>] clients disable --id <id>
                  SQLiteAdminCli [--db <path>] services add --id <id> --display-name <name> --secret <secret> [--endpoint <uri>]
                  SQLiteAdminCli [--db <path>] services list
                  SQLiteAdminCli [--db <path>] services enable --id <id>
                  SQLiteAdminCli [--db <path>] services disable --id <id>
                  SQLiteAdminCli [--db <path>] audit list [--limit <n>]
                  SQLiteAdminCli [--db <path>] audit by-request --request-id <id>
                  SQLiteAdminCli [--db <path>] audit by-client --client-id <id>
                  SQLiteAdminCli [--db <path>] audit by-service --service-id <id>
                """);
    }

    private record CliArgs(List<String> command, List<String> raw) {
        static CliArgs parse(String[] args) {
            List<String> raw = Arrays.asList(args);
            int commandStart = 0;
            while (commandStart < raw.size() && raw.get(commandStart).startsWith("--")) {
                commandStart += optionWidth(raw, commandStart);
            }
            return new CliArgs(raw.subList(commandStart, raw.size()), raw);
        }

        Path databasePath() {
            return Path.of(value("--db", defaultDatabasePath()));
        }

        Path migrationsPath() {
            return Path.of(value("--migrations", SQLiteMigrationRunner.defaultMigrationsDirectory().toString()));
        }

        boolean has(String key) {
            return raw.contains(key);
        }

        String require(String key) {
            String value = value(key, null);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required option " + key);
            }
            return value;
        }

        String value(String key, String defaultValue) {
            int index = raw.indexOf(key);
            if (index < 0 || index + 1 >= raw.size()) {
                return defaultValue;
            }
            String value = raw.get(index + 1);
            if (value.startsWith("--")) {
                return defaultValue;
            }
            return value;
        }

        int intValue(String key, int defaultValue) {
            String value = value(key, null);
            if (value == null) {
                return defaultValue;
            }
            return Integer.parseInt(value);
        }

        private static int optionWidth(List<String> raw, int index) {
            if (index + 1 < raw.size() && !raw.get(index + 1).startsWith("--")) {
                return 2;
            }
            return 1;
        }

        private static String defaultDatabasePath() {
            String configured = System.getenv(AuthConfig.ENV_SQLITE_PATH);
            if (configured == null || configured.isBlank()) {
                return AuthConfig.DEFAULT_LOCAL_SQLITE_PATH;
            }
            return configured;
        }
    }
}
