package br.com.lfmelo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.enums.StaffRole;
import br.com.lfmelo.repository.StaffMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StaffRoleAccessTest {

    @Mock private StaffMemberRepository repository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldResolveActiveStaffRegardlessOfRole(StaffRole role) {
        UUID condominiumId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        StaffMember staff = new StaffMember();
        staff.setId(staffId);
        staff.setUser(user);
        staff.setRole(role);
        AuthenticatedStaff principal = new AuthenticatedStaff(userId, staffId, condominiumId, "staff@example.test");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));

        assertThat(new CurrentStaffResolver(repository).requireActive(condominiumId)).isSameAs(staff);
    }
}
