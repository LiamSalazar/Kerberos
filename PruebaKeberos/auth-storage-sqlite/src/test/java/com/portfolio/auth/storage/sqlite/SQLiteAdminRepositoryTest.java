package com.portfolio.auth.storage.sqlite;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLiteAdminRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void shouldRegisterListAndToggleClientsAndServices() throws Exception {
        Path database = SQLiteTestSupport.initializedDatabase(tempDir, "admin-test.sqlite");
        SQLiteAdminRepository admin = new SQLiteAdminRepository(database);
        SQLitePrincipalRepository principals = new SQLitePrincipalRepository(database);
        SQLiteServiceRegistry services = new SQLiteServiceRegistry(database);

        admin.upsertClient("sample-client", "Sample Client", "sample-client-secret", true);
        admin.upsertService("sample-service", "Sample Service", "sample-service-secret", "local://sample", true);

        assertEquals("sample-client-secret", principals.clientSecret("sample-client").orElseThrow());
        assertEquals("sample-service-secret", services.serviceSecret("sample-service").orElseThrow());
        assertTrue(admin.listClients().stream().anyMatch(client -> "sample-client".equals(client.id())));
        assertTrue(admin.listServices().stream().anyMatch(service -> "sample-service".equals(service.id())));

        assertTrue(admin.setClientEnabled("sample-client", false));
        assertTrue(admin.setServiceEnabled("sample-service", false));

        assertFalse(principals.clientSecret("sample-client").isPresent());
        assertFalse(services.serviceSecret("sample-service").isPresent());
    }
}
