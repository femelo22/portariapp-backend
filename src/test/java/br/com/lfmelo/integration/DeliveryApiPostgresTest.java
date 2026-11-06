package br.com.lfmelo.integration;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeliveryApiPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final Path EVIDENCE_DIRECTORY = Path.of("target", "delivery-api-evidence-" + UUID.randomUUID());

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("evidences.storage-directory", () -> EVIDENCE_DIRECTORY.toString());
    }

    @AfterAll
    static void removeEvidenceDirectory() throws Exception {
        if (Files.exists(EVIDENCE_DIRECTORY)) {
            List<Path> pathsToDelete;
            try (var paths = Files.walk(EVIDENCE_DIRECTORY)) {
                pathsToDelete = paths.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path path : pathsToDelete) {
                Files.deleteIfExists(path);
            }
        }
    }

    @AfterEach
    void removeEvidenceFiles() throws Exception {
        removeEvidenceDirectory();
    }

    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    JwtService jwtService;
    @Autowired
    ObjectMapper json;

    private Tenant a;
    private Tenant b;
    private UUID otherResident;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE delivery_events, delivery_evidences, deliveries, residents, staff_members, units, users, condominiums CASCADE");
        a = tenant("a");
        b = tenant("b");
        otherResident = UUID.randomUUID();
        jdbc.update("INSERT INTO residents(id, condominium_id, unit_id, full_name) VALUES (?, ?, ?, ?)",
                otherResident, a.condominium(), a.unit(), "Other resident");
    }

    @Test
    void requiresAuthenticationForDeliveries() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path(a)))
                .timeout(Duration.ofSeconds(30)).GET().build();
        assertEquals(401, HTTP.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test
    void createsAndPicksUpWithOrderedPersistedHistory() throws Exception {
        UUID id = create(a, a.resident(), "parcel");
        JsonNode received = body(send(a, "GET", path(a) + "/" + id, null), 200);
        assertEquals("RECEIVED", received.get("status").asText());
        assertEquals(a.staff().toString(), received.get("receivedByStaffId").asText());
        assertEquals(a.resident().toString(), received.get("recipientResidentId").asText());
        assertNotNull(Instant.parse(received.get("receivedAt").asText()));

        JsonNode pickedUp = body(send(a, "POST", path(a) + "/" + id + "/pickup",
                Map.of("pickedUpByName", "Maria", "notes", "collected")), 200);
        assertEquals("PICKED_UP", pickedUp.get("status").asText());
        assertEquals("Maria", pickedUp.get("pickedUpByName").asText());
        assertEquals(a.staff().toString(), pickedUp.get("pickedUpByStaffId").asText());
        Instant pickedAt = Instant.parse(pickedUp.get("pickedUpAt").asText());

        JsonNode events = body(send(a, "GET", path(a) + "/" + id + "/events", null), 200);
        assertEquals(2, events.size());
        assertEvent(events.get(0), id, 1, null, "RECEIVED");
        assertEvent(events.get(1), id, 2, "RECEIVED", "PICKED_UP");
        assertEquals("collected", events.get(1).get("notes").asText());
        Instant eventTime = Instant.parse(events.get(1).get("occurredAt").asText());
        assertTrue(Duration.between(pickedAt, eventTime).abs().compareTo(Duration.ofMillis(1)) < 0);
        assertEquals(409, send(a, "POST", path(a) + "/" + id + "/pickup", Map.of("pickedUpByName", "Maria")).statusCode());
        assertEquals(2, eventCount(id));
        assertEquals("PICKED_UP", storedStatus(id));
    }

    @Test
    void isolatesTenantsAndAppliesCombinedFilters() throws Exception {
        UUID picked = create(a, a.resident(), "picked");
        UUID received = create(a, a.resident(), "received");
        UUID other = create(a, otherResident, "other");
        UUID foreign = create(b, b.resident(), "foreign");
        body(send(a, "POST", path(a) + "/" + picked + "/pickup", Map.of("pickedUpByName", "Maria")), 200);

        assertIds(body(send(a, "GET", path(a) + "?status=RECEIVED&recipientResidentId=" + a.resident(), null), 200), received);
        assertIds(body(send(a, "GET", path(a) + "?status=RECEIVED", null), 200), received, other);
        assertIds(body(send(a, "GET", path(a) + "?recipientResidentId=" + a.resident(), null), 200), picked, received);
        assertIds(body(send(b, "GET", path(b), null), 200), foreign);
        assertEquals(403, send(a, "GET", path(b) + "/" + foreign, null).statusCode());
        assertEquals(404, send(a, "GET", path(a) + "/" + foreign, null).statusCode());
        assertEquals(404, send(a, "GET", path(a) + "/" + foreign + "/events", null).statusCode());
        assertEquals(404, send(a, "POST", path(a) + "/" + foreign + "/cancel", Map.of()).statusCode());
        assertEquals(404, send(a, "POST", path(a), Map.of("recipientResidentId", b.resident().toString())).statusCode());
        assertEquals("RECEIVED", storedStatus(foreign));
        assertEquals(1, eventCount(foreign));
    }

    @Test
    void allTerminalTransitionsRecordHistoryAndRejectFurtherChanges() throws Exception {
        for (String[] transition : List.of(
                new String[]{"return", "RETURNED"}, new String[]{"lost", "LOST"}, new String[]{"cancel", "CANCELLED"})) {
            UUID id = create(a, a.resident(), transition[0]);
            assertEquals(transition[1], body(send(a, "POST", path(a) + "/" + id + "/" + transition[0],
                    Map.of("notes", "reason")), 200).get("status").asText());
            JsonNode events = body(send(a, "GET", path(a) + "/" + id + "/events", null), 200);
            assertEquals(2, events.size());
            assertEvent(events.get(0), id, 1, null, "RECEIVED");
            assertEvent(events.get(1), id, 2, "RECEIVED", transition[1]);
            assertEquals("reason", events.get(1).get("notes").asText());
            assertEquals(409, send(a, "POST", path(a) + "/" + id + "/lost", Map.of()).statusCode());
            assertEquals(2, eventCount(id));
            assertEquals(transition[1], storedStatus(id));
        }
    }

    @Test
    void uploadsListsAndDownloadsEvidenceAfterFinalStatusWithTenantIsolation() throws Exception {
        UUID delivery = create(a, a.resident(), "evidence");
        UUID otherDelivery = create(a, otherResident, "other delivery");
        UUID foreignDelivery = create(b, b.resident(), "foreign delivery");
        body(send(a, "POST", path(a) + "/" + delivery + "/pickup",
                Map.of("pickedUpByName", "Maria")), 200);
        assertEquals("PICKED_UP", storedStatus(delivery));

        byte[] png = validPng();
        String evidencePath = evidencePath(a, delivery);
        HttpResponse<String> upload = upload(a, evidencePath, png);
        JsonNode metadata = body(upload, 201);
        UUID evidenceId = UUID.fromString(metadata.get("id").asText());
        assertEquals(a.condominium().toString(), metadata.get("condominiumId").asText());
        assertEquals(delivery.toString(), metadata.get("deliveryId").asText());
        assertEquals(a.staff().toString(), metadata.get("uploadedByStaffId").asText());
        assertEquals("proof.png", metadata.get("originalFilename").asText());
        assertEquals("IMAGE_PNG", metadata.get("contentType").asText());
        assertEquals(png.length, metadata.get("sizeBytes").asLong());
        assertNotNull(Instant.parse(metadata.get("createdAt").asText()));
        assertFalse(metadata.has("storageFileKey"));
        assertFalse(metadata.has("key"));

        Map<String, Object> row = jdbc.queryForMap("""
                SELECT condominium_id, delivery_id, uploaded_by_staff_id, original_filename, content_type,
                       size_bytes, storage_file_key, created_at FROM delivery_evidences WHERE id = ?
                """, evidenceId);
        assertEquals(a.condominium(), row.get("condominium_id"));
        assertEquals(delivery, row.get("delivery_id"));
        assertEquals(a.staff(), row.get("uploaded_by_staff_id"));
        assertEquals("proof.png", row.get("original_filename"));
        assertEquals("image/png", row.get("content_type"));
        assertEquals((long) png.length, ((Number) row.get("size_bytes")).longValue());
        assertNotNull(row.get("created_at"));
        String storageKey = (String) row.get("storage_file_key");
        assertNotNull(storageKey);
        assertFalse(upload.body().contains(storageKey));
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM delivery_evidences", Long.class));

        JsonNode listing = body(send(a, "GET", evidencePath, null), 200);
        assertEquals(1, listing.size());
        JsonNode listed = listing.get(0);
        for (String field : List.of("id", "condominiumId", "deliveryId", "uploadedByStaffId",
                "originalFilename", "contentType", "sizeBytes")) {
            assertEquals(metadata.get(field), listed.get(field), field);
        }
        assertTrue(Duration.between(Instant.parse(metadata.get("createdAt").asText()),
                Instant.parse(listed.get("createdAt").asText())).abs().compareTo(Duration.ofMillis(1)) < 0);
        assertFalse(listing.toString().contains(storageKey));
        HttpResponse<byte[]> download = download(a, evidencePath + "/" + evidenceId + "/content");
        assertEquals(200, download.statusCode());
        assertEquals("image/png", download.headers().firstValue("Content-Type").orElseThrow());
        assertArrayEquals(png, download.body());

        assertEquals(0, body(send(a, "GET", evidencePath(a, otherDelivery), null), 200).size());
        assertEquals(0, body(send(b, "GET", evidencePath(b, foreignDelivery), null), 200).size());
        assertEquals(404, download(a, evidencePath(a, otherDelivery) + "/" + evidenceId + "/content").statusCode());
        assertEquals(404, download(b, evidencePath(b, foreignDelivery) + "/" + evidenceId + "/content").statusCode());
        assertEquals(404, send(a, "GET", evidencePath(a, foreignDelivery), null).statusCode());
        assertEquals(404, upload(a, evidencePath(a, foreignDelivery), png).statusCode());
        assertEquals(403, send(b, "GET", evidencePath, null).statusCode());
        assertEquals(403, download(b, evidencePath + "/" + evidenceId + "/content").statusCode());
        assertEquals(403, upload(b, evidencePath, png).statusCode());
        assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM delivery_evidences", Long.class));
        assertEquals("PICKED_UP", storedStatus(delivery));
    }

    @Test
    void competingTransitionsSerializeAndOnlyOneCommits() throws Exception {
        UUID id = create(a, a.resident(), "race");
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (Connection lock = jdbc.getDataSource().getConnection()) {
            lock.setAutoCommit(false);
            try (var statement = lock.prepareStatement("SELECT id FROM deliveries WHERE id = ? FOR UPDATE")) {
                statement.setObject(1, id);
                try (ResultSet rs = statement.executeQuery()) {
                    assertTrue(rs.next());
                }
            }
            Future<HttpResponse<String>> first = executor.submit(() -> raceRequest(ready, start, id, "return"));
            Future<HttpResponse<String>> second = executor.submit(() -> raceRequest(ready, start, id, "lost"));
            try {
                assertTrue(ready.await(10, TimeUnit.SECONDS));
                start.countDown();
                awaitBlockedTransitions();
            } finally {
                lock.commit();
            }
            var responses = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
            assertEquals(List.of(200, 409), responses.stream().map(HttpResponse::statusCode).sorted().toList());
            String winner = body(responses.stream().filter(r -> r.statusCode() == 200).findFirst().orElseThrow(), 200)
                    .get("status").asText();
            assertTrue(List.of("RETURNED", "LOST").contains(winner));
            assertEquals(winner, storedStatus(id));
            JsonNode events = body(send(a, "GET", path(a) + "/" + id + "/events", null), 200);
            assertEquals(2, events.size());
            assertEvent(events.get(0), id, 1, null, "RECEIVED");
            assertEvent(events.get(1), id, 2, "RECEIVED", winner);
            assertEquals(2, eventCount(id));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
    }

    private void awaitBlockedTransitions() throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            Integer waiting = jdbc.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                      AND lower(query) LIKE '%deliveries%'
                    """, Integer.class);
            if (waiting != null && waiting >= 2) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Both transition transactions did not reach the locked delivery row");
    }

    private HttpResponse<String> raceRequest(CountDownLatch ready, CountDownLatch start, UUID id, String action)
            throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Race start timed out");
        }
        return send(a, "POST", path(a) + "/" + id + "/" + action, Map.of());
    }

    private Tenant tenant(String suffix) {
        UUID condominium = UUID.randomUUID();
        UUID unit = UUID.randomUUID();
        UUID resident = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        String email = suffix + "@integration.test";
        jdbc.update("INSERT INTO condominiums(id, name) VALUES (?, ?)", condominium, "Condominium " + suffix);
        jdbc.update("INSERT INTO units(id, condominium_id, identifier) VALUES (?, ?, ?)", unit, condominium, "101");
        jdbc.update("INSERT INTO residents(id, condominium_id, unit_id, full_name) VALUES (?, ?, ?, ?)",
                resident, condominium, unit, "Resident " + suffix);
        jdbc.update("INSERT INTO users(id, email, active) VALUES (?, ?, true)", userId, email);
        jdbc.update("INSERT INTO staff_members(id, condominium_id, user_id, full_name, active, role) VALUES (?, ?, ?, ?, true, 'DOORMAN')",
                staffId, condominium, userId, "Staff " + suffix);
        var condoEntity = new Condominium();
        condoEntity.setId(condominium);
        var user = new User();
        user.setId(userId);
        user.setEmail(email);
        var staff = new StaffMember();
        staff.setId(staffId);
        staff.setCondominium(condoEntity);
        staff.setUser(user);
        return new Tenant(condominium, unit, resident, staffId, jwtService.generateToken(staff));
    }

    private UUID create(Tenant tenant, UUID resident, String description) throws Exception {
        JsonNode created = body(send(tenant, "POST", path(tenant),
                Map.of("recipientResidentId", resident.toString(), "description", description)), 201);
        assertEquals("RECEIVED", created.get("status").asText());
        return UUID.fromString(created.get("id").asText());
    }

    private HttpResponse<String> send(Tenant tenant, String method, String path, Object data) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + tenant.token());
        if (data == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> upload(Tenant tenant, String path, byte[] png) throws Exception {
        String boundary = "test-" + UUID.randomUUID();
        byte[] header = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"proof.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
        byte[] footer = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII);
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + tenant.token())
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(header),
                        HttpRequest.BodyPublishers.ofByteArray(png), HttpRequest.BodyPublishers.ofByteArray(footer)))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<byte[]> download(Tenant tenant, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + tenant.token()).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static byte[] validPng() throws Exception {
        var image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0x123456);
        var bytes = new ByteArrayOutputStream();
        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            assertTrue(ImageIO.write(image, "png", output));
        }
        return bytes.toByteArray();
    }

    private JsonNode body(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private void assertEvent(JsonNode event, UUID id, long sequence, String previous, String status) {
        assertEquals(id.toString(), event.get("deliveryId").asText());
        assertEquals(a.condominium().toString(), event.get("condominiumId").asText());
        assertEquals(a.staff().toString(), event.get("staffId").asText());
        assertEquals(sequence, event.get("sequenceNumber").asLong());
        assertEquals(status, event.get("status").asText());
        if (previous == null) {
            assertTrue(event.get("previousStatus").isNull());
        } else {
            assertEquals(previous, event.get("previousStatus").asText());
        }
        assertNotNull(Instant.parse(event.get("occurredAt").asText()));
    }

    private static void assertIds(JsonNode result, UUID... ids) {
        var actual = new java.util.HashSet<UUID>();
        result.forEach(item -> actual.add(UUID.fromString(item.get("id").asText())));
        assertEquals(ids.length, result.size());
        assertEquals(java.util.Set.of(ids), actual);
    }

    private long eventCount(UUID id) {
        return jdbc.queryForObject("SELECT count(*) FROM delivery_events WHERE delivery_id = ?", Long.class, id);
    }

    private String storedStatus(UUID id) {
        return jdbc.queryForObject("SELECT status FROM deliveries WHERE id = ?", String.class, id);
    }

    private static String path(Tenant tenant) {
        return "/api/v1/condominiums/" + tenant.condominium() + "/deliveries";
    }

    private static String evidencePath(Tenant tenant, UUID delivery) {
        return path(tenant) + "/" + delivery + "/evidences";
    }

    private record Tenant(UUID condominium, UUID unit, UUID resident, UUID staff, String token) {
    }
}
