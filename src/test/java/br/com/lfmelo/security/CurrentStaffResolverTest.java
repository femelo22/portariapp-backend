package br.com.lfmelo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.exception.ForbiddenException;
import br.com.lfmelo.exception.UnauthorizedException;
import br.com.lfmelo.repository.StaffMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CurrentStaffResolverTest {

    @Mock private StaffMemberRepository repository;
    private final UUID condominiumId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private CurrentStaffResolver resolver;
    private StaffMember staff;
    private User user;

    @BeforeEach
    void setUp() {
        resolver = new CurrentStaffResolver(repository);
        user = new User();
        user.setId(userId);
        staff = new StaffMember();
        staff.setId(staffId);
        staff.setUser(user);
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveActiveStaffFromPrincipal() {
        authenticate(condominiumId, userId);
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));

        assertThat(resolver.requireActive(condominiumId)).isSameAs(staff);
    }

    @Test
    void shouldRejectMissingPrincipal() {
        assertThatThrownBy(() -> resolver.requireActive(condominiumId))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldRejectAnotherCondominium() {
        authenticate(UUID.randomUUID(), userId);
        assertThatThrownBy(() -> resolver.requireActive(condominiumId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectInactiveStaff() {
        authenticate(condominiumId, userId);
        staff.setActive(false);
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> resolver.requireActive(condominiumId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectInactiveUser() {
        authenticate(condominiumId, userId);
        user.setActive(false);
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> resolver.requireActive(condominiumId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void shouldRejectChangedUserLink() {
        authenticate(condominiumId, UUID.randomUUID());
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));

        assertThatThrownBy(() -> resolver.requireActive(condominiumId))
                .isInstanceOf(ForbiddenException.class);
    }

    private void authenticate(UUID tenant, UUID principalUserId) {
        AuthenticatedStaff principal = new AuthenticatedStaff(principalUserId, staffId, tenant, "staff@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
