package br.com.lfmelo.integration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffRoleApiPostgresTest {

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private static final String PASSWORD = "integration-password";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    ObjectMapper json;

    private UUID condominium;
    private UUID otherCondominium;
    private UUID otherStaff;
    private UUID resident;
    private Staff doorman;
    private Staff admin;

    @BeforeEach
    void seed() {
        condominium = UUID.randomUUID();
        otherCondominium = UUID.randomUUID();
        jdbc.update("INSERT INTO condominiums(id, name) VALUES (?, ?)", condominium, "Condo " + condominium);
        jdbc.update("INSERT INTO condominiums(id, name) VALUES (?, ?)", otherCondominium, "Condo " + otherCondominium);
        otherStaff = UUID.randomUUID();
        jdbc.update("INSERT INTO staff_members(id, condominium_id, full_name, role) VALUES (?, ?, ?, ?)",
                otherStaff, otherCondominium, "Foreign staff", "DOORMAN");
        UUID unit = UUID.randomUUID();
        resident = UUID.randomUUID();
        jdbc.update("INSERT INTO units(id, condominium_id, identifier) VALUES (?, ?, ?)", unit, condominium, "101");
        jdbc.update("INSERT INTO residents(id, condominium_id, unit_id, full_name) VALUES (?, ?, ?, ?)",
                resident, condominium, unit, "Resident");
        doorman = staff("DOORMAN");
        admin = staff("ADMIN");
    }

    @Test
    void createsUpdatesAndReadsBothRolesAndRejectsMissingNullOrInvalidRoles() throws Exception {
        String token = login(doorman, "DOORMAN");
        String base = staffPath(condominium);
        Map<String, Object> request = new HashMap<>();
        request.put("condominiumId", condominium.toString());
        request.put("fullName", "New staff");
        for (String invalid : new String[]{null, "MANAGER"}) {
            request.put("role", invalid);
            assertEquals(400, send("POST", base, token, request).statusCode());
        }
        request.remove("role");
        assertEquals(400, send("POST", base, token, request).statusCode());

        request.put("role", "DOORMAN");
        JsonNode created = body(send("POST", base, token, request), 201);
        String id = created.get("id").asText();
        assertEquals("DOORMAN", created.get("role").asText());
        assertEquals("DOORMAN", storedRole(UUID.fromString(id)));

        Map<String, Object> update = new HashMap<>();
        update.put("fullName", "Updated staff");
        update.put("active", true);
        for (String invalid : new String[]{null, "MANAGER"}) {
            update.put("role", invalid);
            assertEquals(400, send("PUT", base + "/" + id, token, update).statusCode());
        }
        update.remove("role");
        assertEquals(400, send("PUT", base + "/" + id, token, update).statusCode());
        assertEquals("DOORMAN", storedRole(UUID.fromString(id)));

        update.put("role", "ADMIN");
        assertEquals("ADMIN", body(send("PUT", base + "/" + id, token, update), 200).get("role").asText());
        assertEquals("ADMIN", storedRole(UUID.fromString(id)));
        assertEquals("ADMIN", body(send("GET", base + "/" + id, token, null), 200).get("role").asText());
        JsonNode list = body(send("GET", base, token, null), 200);
        assertTrue(list.isArray());
        assertTrue(list.findValues("role").stream().anyMatch(role -> "ADMIN".equals(role.asText())));
        assertTrue(list.findValues("role").stream().anyMatch(role -> "DOORMAN".equals(role.asText())));

        request.put("role", "ADMIN");
        JsonNode createdAdmin = body(send("POST", base, token, request), 201);
        assertEquals("ADMIN", createdAdmin.get("role").asText());
        assertEquals("ADMIN", storedRole(UUID.fromString(createdAdmin.get("id").asText())));
    }

    @Test
    void bothRolesHaveTheSamePermissionsAndCannotAccessAnotherCondominium() throws Exception {
        String doormanToken = login(doorman, "DOORMAN");
        String adminToken = login(admin, "ADMIN");
        String condominiums = "/api/v1/condominiums";
        String ownStaff = staffPath(condominium);
        String foreignStaff = staffPath(otherCondominium);
        Map<String, Object> ownRequest = Map.of(
                "condominiumId", condominium.toString(), "fullName", "Role access test", "role", "DOORMAN");
        Map<String, Object> foreignRequest = Map.of(
                "condominiumId", otherCondominium.toString(), "fullName", "Foreign create", "role", "ADMIN");
        Map<String, Object> update = Map.of("fullName", "Role access update", "active", true, "role", "ADMIN");

        for (String token : new String[]{doormanToken, adminToken}) {
            assertEquals(200, send("GET", condominiums + "/" + condominium, token, null).statusCode());
            assertEquals(403, send("GET", condominiums, token, null).statusCode());
            assertEquals(403, send("POST", condominiums, token, Map.of("name", "Not allowed")).statusCode());
            assertEquals(403, send("DELETE", condominiums + "/" + condominium, token, null).statusCode());
            assertEquals(403, send("GET", condominiums + "/" + otherCondominium, token, null).statusCode());
            assertEquals(403, send("PUT", condominiums + "/" + otherCondominium, token,
                    Map.of("name", "Not allowed")).statusCode());

            assertEquals(200, send("GET", ownStaff, token, null).statusCode());
            JsonNode created = body(send("POST", ownStaff, token, ownRequest), 201);
            String id = created.get("id").asText();
            assertEquals("ADMIN", body(send("PUT", ownStaff + "/" + id, token, update), 200).get("role").asText());
            assertEquals(204, send("DELETE", ownStaff + "/" + id, token, null).statusCode());

            assertEquals(403, send("GET", foreignStaff, token, null).statusCode());
            assertEquals(403, send("GET", foreignStaff + "/" + otherStaff, token, null).statusCode());
            assertEquals(403, send("POST", foreignStaff, token, foreignRequest).statusCode());
            assertEquals(403, send("PUT", foreignStaff + "/" + otherStaff, token, update).statusCode());
            assertEquals(403, send("DELETE", foreignStaff + "/" + otherStaff, token, null).statusCode());
            assertEquals(403, send("POST", deliveryPath(otherCondominium), token,
                    Map.of("recipientResidentId", resident.toString())).statusCode());
        }
        assertEquals("DOORMAN", storedRole(otherStaff));
        assertEquals("Foreign staff",
                jdbc.queryForObject("SELECT full_name FROM staff_members WHERE id = ?", String.class, otherStaff));
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM staff_members WHERE condominium_id = ?", Long.class, otherCondominium));
    }

    @Test
    void loginReportsRoleWithoutChangingJwtClaimsOrTenantAuthorization() throws Exception {
        String doormanToken = login(doorman, "DOORMAN");
        String adminToken = login(admin, "ADMIN");
        for (Staff staff : new Staff[]{doorman, admin}) {
            String token = staff == doorman ? doormanToken : adminToken;
            JsonNode claims = json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
            Set<String> claimNames = new HashSet<>();
            claims.fieldNames().forEachRemaining(claimNames::add);
            assertEquals(Set.of("sub", "userId", "staffId", "condominiumId", "iat", "exp"), claimNames);
            assertEquals(staff.email(), claims.get("sub").asText());
            assertEquals(staff.userId().toString(), claims.get("userId").asText());
            assertEquals(staff.id().toString(), claims.get("staffId").asText());
            assertEquals(condominium.toString(), claims.get("condominiumId").asText());
            assertEquals(200, send("GET", deliveryPath(condominium), token, null).statusCode());
            assertEquals(201, createDelivery(token).statusCode());
            assertEquals(403, send("GET", deliveryPath(otherCondominium), token, null).statusCode());
        }
        assertEquals(401, send("GET", deliveryPath(condominium), null, null).statusCode());
        assertEquals(401, send("POST", "/api/v1/auth/login", null,
                Map.of("email", doorman.email(), "password", "incorrect")).statusCode());

        jdbc.update("UPDATE staff_members SET role = 'ADMIN' WHERE id = ?", doorman.id());
        assertEquals(200, send("GET", deliveryPath(condominium), doormanToken, null).statusCode());
        assertEquals(201, createDelivery(doormanToken).statusCode());
        assertEquals(403, send("GET", deliveryPath(otherCondominium), doormanToken, null).statusCode());
        login(doorman, "ADMIN");
        jdbc.update("UPDATE staff_members SET active = false WHERE id = ?", doorman.id());
        assertEquals(403, createDelivery(doormanToken).statusCode());
        assertEquals(401, send("POST", "/api/v1/auth/login", null,
                Map.of("email", doorman.email(), "password", PASSWORD)).statusCode());
        assertEquals(200, send("GET", deliveryPath(condominium), adminToken, null).statusCode());
    }

    private HttpResponse<String> createDelivery(String token) throws Exception {
        return send("POST", deliveryPath(condominium), token,
                Map.of("recipientResidentId", resident.toString(), "description", "role test"));
    }

    private Staff staff(String role) {
        UUID userId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        String email = staffId + "@integration.test";
        jdbc.update("INSERT INTO users(id, email, password_hash, active) VALUES (?, ?, ?, true)",
                userId, email, passwordEncoder.encode(PASSWORD));
        jdbc.update("INSERT INTO staff_members(id, condominium_id, user_id, full_name, active, role) VALUES (?, ?, ?, ?, true, ?)",
                staffId, condominium, userId, role + " staff", role);
        return new Staff(staffId, userId, email);
    }

    private String login(Staff staff, String role) throws Exception {
        JsonNode response = body(send("POST", "/api/v1/auth/login", null,
                Map.of("email", staff.email(), "password", PASSWORD)), 200);
        assertEquals(role, response.get("role").asText());
        assertEquals(staff.id().toString(), response.get("staffMemberId").asText());
        assertEquals(condominium.toString(), response.get("condominiumId").asText());
        assertEquals("Bearer", response.get("tokenType").asText());
        assertTrue(response.get("token").isTextual());
        return response.get("token").asText();
    }

    private String storedRole(UUID staffId) {
        return jdbc.queryForObject("SELECT role FROM staff_members WHERE id = ?", String.class, staffId);
    }

    private HttpResponse<String> send(String method, String path, String token, Object data) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (data == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(data)));
        }
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode body(HttpResponse<String> response, int status) throws Exception {
        assertEquals(status, response.statusCode(), response.body());
        return json.readTree(response.body());
    }

    private static String staffPath(UUID condominium) {
        return "/api/v1/condominiums/" + condominium + "/staff-members";
    }

    private static String deliveryPath(UUID condominium) {
        return "/api/v1/condominiums/" + condominium + "/deliveries";
    }

    private record Staff(UUID id, UUID userId, String email) {
    }
}
