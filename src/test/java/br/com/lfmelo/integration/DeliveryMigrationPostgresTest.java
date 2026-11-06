package br.com.lfmelo.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class DeliveryMigrationPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private String schema;

    @BeforeEach
    void migrateLegacySchema() {
        schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        assertEquals(2, flyway("2").migrate().migrationsExecuted);
    }

    @Test
    void emptyLegacyDatabaseMigratesAndCreatesEventSchema() throws Exception {
        assertEquals(1, flyway("3").migrate().migrationsExecuted);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("""
                    SELECT column_name FROM information_schema.columns
                    WHERE table_schema = current_schema() AND table_name = 'delivery_events'
                    """)) {
                var columns = new java.util.HashSet<String>();
                while (rs.next()) {
                    columns.add(rs.getString(1));
                }
                assertTrue(columns.containsAll(java.util.Set.of("id", "condominium_id", "delivery_id",
                        "sequence_number", "previous_status", "status", "staff_id", "occurred_at", "notes")));
            }
            assertEquals(3, count(statement, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL"));
            assertEquals(0, count(statement, "SELECT count(*) FROM delivery_events"));
            seedLegacyDelivery(statement, false);
            statement.executeUpdate("""
                    INSERT INTO delivery_events(condominium_id, delivery_id, sequence_number, previous_status, status, staff_id, occurred_at)
                    VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000005',
                            1, NULL, 'RECEIVED', '00000000-0000-0000-0000-000000000004', '2026-01-01T12:00:00Z')
                    """);
            assertEquals(1, count(statement, """
                    SELECT count(*) FROM delivery_events WHERE sequence_number = 1 AND previous_status IS NULL
                    AND status = 'RECEIVED' AND staff_id = '00000000-0000-0000-0000-000000000004'
                    AND occurred_at = '2026-01-01T12:00:00Z'::timestamptz
                    """));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO delivery_events(condominium_id, delivery_id, sequence_number, status, staff_id, occurred_at)
                    SELECT condominium_id, delivery_id, sequence_number, status, staff_id, occurred_at FROM delivery_events
                    """));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO delivery_events(condominium_id, delivery_id, sequence_number, previous_status, status, staff_id, occurred_at)
                    SELECT condominium_id, delivery_id, 2, status, 'INVALID', staff_id, occurred_at FROM delivery_events
                    """));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO delivery_events(condominium_id, delivery_id, sequence_number, previous_status, status, staff_id, occurred_at)
                    SELECT condominium_id, delivery_id, 2, status, 'LOST', gen_random_uuid(), occurred_at FROM delivery_events
                    """));
            assertEquals(1, count(statement, "SELECT count(*) FROM delivery_events"));
        }
    }

    @Test
    void migrationRejectsExistingDeliveriesWithoutLosingData() throws Exception {
        verifyBlockedMigration(false);
    }

    @Test
    void migrationRejectsExistingEvidenceWithoutLosingEitherRecord() throws Exception {
        verifyBlockedMigration(true);
    }

    private void verifyBlockedMigration(boolean withEvidence) throws Exception {
        String deliveriesBefore;
        String evidencesBefore;
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            seedLegacyDelivery(statement, withEvidence);
            deliveriesBefore = snapshot(statement, "deliveries");
            evidencesBefore = snapshot(statement, "delivery_evidences");
        }
        FlywayException failure = assertThrows(FlywayException.class, () -> flyway("3").migrate());
        assertTrue(failure.getMessage().contains("deliveries and delivery_evidences must both be empty"),
                failure.getMessage());
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(deliveriesBefore, snapshot(statement, "deliveries"));
            assertEquals(evidencesBefore, snapshot(statement, "delivery_evidences"));
            assertEquals(1, count(statement, "SELECT count(*) FROM deliveries WHERE description = 'legacy parcel' AND status = 'RECEIVED'"));
            assertEquals(withEvidence ? 1 : 0, count(statement,
                    "SELECT count(*) FROM delivery_evidences WHERE storage_file_key = 'legacy-file' AND size_bytes = 12"));
            assertEquals(2, count(statement, "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL"));
            assertEquals(0, count(statement, """
                    SELECT count(*) FROM information_schema.tables
                    WHERE table_schema = current_schema() AND table_name = 'delivery_events'
                    """));
            statement.executeUpdate("DELETE FROM delivery_evidences WHERE delivery_id = '00000000-0000-0000-0000-000000000005'");
            statement.executeUpdate("DELETE FROM deliveries WHERE id = '00000000-0000-0000-0000-000000000005'");
        }
        assertEquals(1, flyway("3").migrate().migrationsExecuted);
    }

    private Flyway flyway(String target) {
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private Connection connection() throws Exception {
        Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        connection.setSchema(schema);
        return connection;
    }

    private static long count(Statement statement, String sql) throws Exception {
        try (ResultSet rs = statement.executeQuery(sql)) {
            assertTrue(rs.next());
            return rs.getLong(1);
        }
    }

    private static String snapshot(Statement statement, String table) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT COALESCE(json_agg(t ORDER BY t.id)::text, '[]') FROM " + table + " t")) {
            assertTrue(rs.next());
            return rs.getString(1);
        }
    }

    private static void seedLegacyDelivery(Statement statement, boolean withEvidence) throws Exception {
        statement.executeUpdate("INSERT INTO condominiums(id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Legacy')");
        statement.executeUpdate("INSERT INTO units(id, condominium_id, identifier) VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', '1')");
        statement.executeUpdate("INSERT INTO residents(id, condominium_id, unit_id, full_name) VALUES ('00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000002', 'Resident')");
        statement.executeUpdate("INSERT INTO staff_members(id, condominium_id, full_name) VALUES ('00000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000001', 'Staff')");
        statement.executeUpdate("INSERT INTO deliveries(id, condominium_id, recipient_resident_id, received_by_staff_id, description) VALUES ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000004', 'legacy parcel')");
        if (withEvidence) {
            statement.executeUpdate("INSERT INTO delivery_evidences(condominium_id, delivery_id, uploaded_by_staff_id, storage_file_key, content_type, size_bytes) VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000004', 'legacy-file', 'image/png', 12)");
        }
    }
}
