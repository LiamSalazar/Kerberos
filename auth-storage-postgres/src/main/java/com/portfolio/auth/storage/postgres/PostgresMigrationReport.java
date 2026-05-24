package com.portfolio.auth.storage.postgres;

import java.util.List;

public record PostgresMigrationReport(
        int appliedCount,
        int skippedCount,
        List<String> appliedVersions
) {
    public PostgresMigrationReport {
        appliedVersions = List.copyOf(appliedVersions);
    }
}
