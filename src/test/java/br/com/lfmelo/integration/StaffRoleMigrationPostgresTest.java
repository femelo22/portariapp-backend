package br.com.lfmelo.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class StaffRoleMigrationPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    private static final List<String> TABLES = List.of("condominiums", "units", "users", "residents",
            "staff_members", "deliveries", "delivery_evidences", "delivery_events");
    private String schema;

    @BeforeEach
    void migrateToV3() {
        schema = "staff_role_" + UUID.randomUUID().toString().replace("-", "");
        assertEquals(3, flyway("3").migrate().migrationsExecuted);
    }

    @Test
    void v4BackfillsStaffAndPreservesExistingRowsAndRelationships() throws Exception {
        Map<String, String> before = new LinkedHashMap<>();
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            seedV3(statement);
            for (String table : TABLES) {
                before.put(table, snapshot(statement, table, false));
            }
        }

        assertEquals(1, flyway("4").migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                assertEquals(before.get(table), snapshot(statement, table, table.equals("staff_members")), table);
            }
            assertEquals(2, count(statement, "SELECT count(*) FROM staff_members WHERE role = 'DOORMAN'"));
            assertEquals(4, count(statement,
                    "SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL"));
            assertEquals(1, count(statement, """
                    SELECT count(*) FROM deliveries d
                    JOIN residents r ON r.id = d.recipient_resident_id AND r.condominium_id = d.condominium_id
                    JOIN units u ON u.id = r.unit_id AND u.condominium_id = r.condominium_id
                    JOIN users ru ON ru.id = r.user_id
                    JOIN staff_members receiver ON receiver.id = d.received_by_staff_id AND receiver.condominium_id = d.condominium_id
                    JOIN users su ON su.id = receiver.user_id
                    JOIN staff_members picker ON picker.id = d.picked_up_by_staff_id AND picker.condominium_id = d.condominium_id
                    JOIN delivery_evidences e ON e.delivery_id = d.id AND e.condominium_id = d.condominium_id
                    JOIN delivery_events ev ON ev.delivery_id = d.id AND ev.condominium_id = d.condominium_id
                    WHERE d.description = 'parcel' AND d.status = 'PICKED_UP'
                      AND e.uploaded_by_staff_id = receiver.id AND ev.staff_id = picker.id
                      AND ev.sequence_number = 2 AND ev.status = 'PICKED_UP'
                      AND ru.email = 'resident@example.test' AND su.email = 'staff@example.test'
                    """));
        }
    }

    @Test
    void v4RequiresExplicitValidRoleOnInsertsAndUpdates() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            seedV3(statement);
        }
        assertEquals(1, flyway("4").migrate().migrationsExecuted);

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            assertEquals(1, count(statement, """
                    SELECT count(*) FROM information_schema.columns
                    WHERE table_schema = current_schema() AND table_name = 'staff_members' AND column_name = 'role'
                      AND data_type = 'character varying' AND is_nullable = 'NO' AND column_default IS NULL
                    """));
            String prefix = "INSERT INTO staff_members(condominium_id, full_name";
            String condo = "'00000000-0000-0000-0000-000000000001'";
            assertSqlState("23502", () -> statement.executeUpdate(prefix + ") VALUES (" + condo + ", 'missing')"));
            assertSqlState("23502", () -> statement.executeUpdate(prefix + ", role) VALUES (" + condo + ", 'null', NULL)"));
            assertSqlState("23514", () -> statement.executeUpdate(prefix + ", role) VALUES (" + condo + ", 'invalid', 'MANAGER')"));
            assertSqlState("23502", () -> statement.executeUpdate(
                    "UPDATE staff_members SET role = NULL WHERE full_name = 'Receiver'"));
            assertSqlState("23514", () -> statement.executeUpdate(
                    "UPDATE staff_members SET role = 'INVALID' WHERE full_name = 'Receiver'"));

            assertEquals(1, statement.executeUpdate(prefix + ", role) VALUES (" + condo + ", 'New doorman', 'DOORMAN')"));
            assertEquals(1, statement.executeUpdate(prefix + ", role) VALUES (" + condo + ", 'New admin', 'ADMIN')"));
            assertEquals(1, statement.executeUpdate("UPDATE staff_members SET role = 'ADMIN' WHERE full_name = 'Picker'"));
            assertEquals(2, count(statement, "SELECT count(*) FROM staff_members WHERE role = 'DOORMAN'"));
            assertEquals(2, count(statement, "SELECT count(*) FROM staff_members WHERE role = 'ADMIN'"));
            assertEquals(4, count(statement, "SELECT count(*) FROM staff_members"));
        }
    }

    private Flyway flyway(String target) {
        return Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .schemas(schema).defaultSchema(schema).locations("classpath:db/migration")
                .target(target).load();
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

    private static String snapshot(Statement statement, String table, boolean excludeRole) throws Exception {
        String row = excludeRole ? "(to_jsonb(t) - 'role')" : "to_jsonb(t)";
        try (ResultSet rs = statement.executeQuery(
                "SELECT COALESCE(jsonb_agg(" + row + " ORDER BY t.id)::text, '[]') FROM " + table + " t")) {
            assertTrue(rs.next());
            return rs.getString(1);
        }
    }

    private static void assertSqlState(String state, SqlOperation operation) {
        SQLException failure = assertThrows(SQLException.class, operation::run);
        assertEquals(state, failure.getSQLState(), failure.getMessage());
    }

    @FunctionalInterface
    private interface SqlOperation {
        void run() throws SQLException;
    }

    private static void seedV3(Statement statement) throws SQLException {
        statement.executeUpdate("INSERT INTO condominiums(id, name) VALUES ('00000000-0000-0000-0000-000000000001', 'Legacy condo')");
        statement.executeUpdate("INSERT INTO units(id, condominium_id, identifier) VALUES ('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000001', '101')");
        statement.executeUpdate("""
                INSERT INTO users(id, email, password_hash, active) VALUES
                ('00000000-0000-0000-0000-000000000003', 'resident@example.test', 'resident-hash', true),
                ('00000000-0000-0000-0000-000000000004', 'staff@example.test', 'staff-hash', true)
                """);
        statement.executeUpdate("""
                INSERT INTO residents(id, condominium_id, unit_id, user_id, full_name, phone)
                VALUES ('00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000001',
                        '00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000003', 'Resident', '123')
                """);
        statement.executeUpdate("""
                INSERT INTO staff_members(id, condominium_id, user_id, full_name, active) VALUES
                ('00000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001',
                 '00000000-0000-0000-0000-000000000004', 'Receiver', true),
                ('00000000-0000-0000-0000-000000000007', '00000000-0000-0000-0000-000000000001',
                 NULL, 'Picker', false)
                """);
        statement.executeUpdate("""
                INSERT INTO deliveries(id, condominium_id, recipient_resident_id, received_by_staff_id, status,
                                       description, picked_up_at, picked_up_by_staff_id, picked_up_by_name, notes)
                VALUES ('00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000001',
                        '00000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000006',
                        'PICKED_UP', 'parcel', '2026-01-02T12:00:00Z',
                        '00000000-0000-0000-0000-000000000007', 'Collector', 'delivery notes')
                """);
        statement.executeUpdate("""
                INSERT INTO delivery_evidences(id, condominium_id, delivery_id, uploaded_by_staff_id,
                                               storage_file_key, original_filename, content_type, size_bytes)
                VALUES ('00000000-0000-0000-0000-000000000009', '00000000-0000-0000-0000-000000000001',
                        '00000000-0000-0000-0000-000000000008', '00000000-0000-0000-0000-000000000006',
                        'proof-key', 'proof.png', 'image/png', 12)
                """);
        statement.executeUpdate("""
                INSERT INTO delivery_events(id, condominium_id, delivery_id, sequence_number, previous_status, status, staff_id, occurred_at, notes)
                VALUES ('00000000-0000-0000-0000-000000000010', '00000000-0000-0000-0000-000000000001',
                        '00000000-0000-0000-0000-000000000008', 1, NULL, 'RECEIVED',
                        '00000000-0000-0000-0000-000000000006', '2026-01-01T12:00:00Z', 'received'),
                       ('00000000-0000-0000-0000-000000000011', '00000000-0000-0000-0000-000000000001',
                        '00000000-0000-0000-0000-000000000008', 2, 'RECEIVED', 'PICKED_UP',
                        '00000000-0000-0000-0000-000000000007', '2026-01-02T12:00:00Z', 'picked up')
                """);
    }
}
