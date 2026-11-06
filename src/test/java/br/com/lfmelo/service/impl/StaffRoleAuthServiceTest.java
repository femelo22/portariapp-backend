package br.com.lfmelo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import br.com.lfmelo.dto.auth.LoginRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.enums.StaffRole;
import br.com.lfmelo.exception.UnauthorizedException;
import br.com.lfmelo.repository.StaffMemberRepository;
import br.com.lfmelo.repository.UserRepository;
import br.com.lfmelo.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class StaffRoleAuthServiceTest {

    private static final String SECRET = "test-only-signing-key-with-at-least-32-bytes";
    private final UUID condominiumId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    @Mock private UserRepository users;
    @Mock private StaffMemberRepository staffMembers;
    @Mock private PasswordEncoder passwords;
    private JwtService jwtService;
    private AuthServiceImpl service;
    private User user;
    private StaffMember staff;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60);
        service = new AuthServiceImpl(users, staffMembers, passwords, jwtService);
        Condominium condominium = new Condominium();
        condominium.setId(condominiumId);
        user = new User();
        user.setId(userId);
        user.setEmail("staff@example.test");
        user.setPasswordHash("hash");
        staff = new StaffMember();
        staff.setId(staffId);
        staff.setCondominium(condominium);
        staff.setUser(user);
        staff.setFullName("Maria");
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldReturnRoleWithoutChangingJwtClaims(StaffRole role) {
        staff.setRole(role);
        stubValidCredentials();
        when(staffMembers.findByUserId(userId)).thenReturn(Optional.of(staff));

        var response = service.login(new LoginRequest(user.getEmail(), "password"));

        assertThat(response.role()).isEqualTo(role);
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.staffMemberId()).isEqualTo(staffId);
        assertThat(response.condominiumId()).isEqualTo(condominiumId);
        assertThat(response.fullName()).isEqualTo("Maria");
        assertThat(response.email()).isEqualTo(user.getEmail());
        assertThat(response.expiresAt()).isNotNull();
        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build().parseSignedClaims(response.token()).getPayload();
        assertThat(claims.keySet()).isEqualTo(Set.of("sub", "userId", "staffId", "condominiumId", "iat", "exp"));
        assertThat(claims.getSubject()).isEqualTo(user.getEmail());
        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("staffId", String.class)).isEqualTo(staffId.toString());
        assertThat(claims.get("condominiumId", String.class)).isEqualTo(condominiumId.toString());
        var principal = jwtService.parseToken(response.token());
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.staffId()).isEqualTo(staffId);
        assertThat(principal.condominiumId()).isEqualTo(condominiumId);
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldStillRejectInactiveStaff(StaffRole role) {
        staff.setRole(role);
        staff.setActive(false);
        stubValidCredentials();
        when(staffMembers.findByUserId(userId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> service.login(new LoginRequest(user.getEmail(), "password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldStillRejectInactiveUser(StaffRole role) {
        staff.setRole(role);
        user.setActive(false);
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest(user.getEmail(), "password")))
                .isInstanceOf(UnauthorizedException.class);
    }

    private void stubValidCredentials() {
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        when(passwords.matches("password", "hash")).thenReturn(true);
    }
}
