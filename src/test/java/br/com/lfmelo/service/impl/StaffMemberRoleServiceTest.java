package br.com.lfmelo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.dto.staffmember.CreateStaffMemberRequest;
import br.com.lfmelo.dto.staffmember.UpdateStaffMemberRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.enums.StaffRole;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.repository.StaffMemberRepository;
import br.com.lfmelo.service.CondominiumService;
import br.com.lfmelo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffMemberRoleServiceTest {

    private final UUID condominiumId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    @Mock private StaffMemberRepository repository;
    @Mock private CondominiumService condominiums;
    @Mock private UserService users;
    private StaffMemberServiceImpl service;
    private Condominium condominium;
    private User user;

    @BeforeEach
    void setUp() {
        service = new StaffMemberServiceImpl(repository, condominiums, users);
        condominium = new Condominium();
        condominium.setId(condominiumId);
        user = new User();
        user.setId(userId);
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldSaveAndReturnRequestedRoleOnCreate(StaffRole role) {
        when(condominiums.getOrThrow(condominiumId)).thenReturn(condominium);
        when(users.getOrThrow(userId)).thenReturn(user);
        when(repository.save(any(StaffMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(condominiumId,
                new CreateStaffMemberRequest(condominiumId, userId, "Maria", role));

        ArgumentCaptor<StaffMember> saved = ArgumentCaptor.forClass(StaffMember.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getRole()).isEqualTo(role);
        assertThat(saved.getValue().getCondominium()).isSameAs(condominium);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(result.role()).isEqualTo(role);
        assertThat(result.fullName()).isEqualTo("Maria");
    }

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldSaveAndReturnChangedRoleOnUpdate(StaffRole role) {
        StaffMember staff = staff(role == StaffRole.ADMIN ? StaffRole.DOORMAN : StaffRole.ADMIN);
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(staff));
        when(repository.save(staff)).thenReturn(staff);

        var result = service.update(condominiumId, staffId, new UpdateStaffMemberRequest("Updated", false, role));

        verify(repository).save(staff);
        assertThat(staff.getRole()).isEqualTo(role);
        assertThat(staff.getFullName()).isEqualTo("Updated");
        assertThat(staff.isActive()).isFalse();
        assertThat(staff.getUser()).isSameAs(user);
        assertThat(result.role()).isEqualTo(role);
    }

    @Test
    void shouldReturnRolesInListAndSingleLookup() {
        StaffMember doorman = staff(StaffRole.DOORMAN);
        StaffMember admin = staff(StaffRole.ADMIN);
        when(condominiums.getOrThrow(condominiumId)).thenReturn(condominium);
        when(repository.findByCondominiumId(condominiumId)).thenReturn(List.of(doorman, admin));
        when(repository.findByIdAndCondominiumId(staffId, condominiumId)).thenReturn(Optional.of(admin));

        assertThat(service.findAll(condominiumId)).extracting(dto -> dto.role())
                .containsExactly(StaffRole.DOORMAN, StaffRole.ADMIN);
        assertThat(service.findById(condominiumId, staffId).role()).isEqualTo(StaffRole.ADMIN);
    }

    @Test
    void shouldStillRejectMismatchedCondominiumOnCreate() {
        var request = new CreateStaffMemberRequest(UUID.randomUUID(), userId, "Maria", StaffRole.ADMIN);
        assertThatThrownBy(() -> service.create(condominiumId, request))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void shouldStillRejectUserAlreadyLinkedToStaff() {
        when(condominiums.getOrThrow(condominiumId)).thenReturn(condominium);
        when(users.getOrThrow(userId)).thenReturn(user);
        when(repository.existsByUserId(userId)).thenReturn(true);
        var request = new CreateStaffMemberRequest(condominiumId, userId, "Maria", StaffRole.DOORMAN);

        assertThatThrownBy(() -> service.create(condominiumId, request))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    private StaffMember staff(StaffRole role) {
        StaffMember staff = new StaffMember();
        staff.setId(staffId);
        staff.setCondominium(condominium);
        staff.setUser(user);
        staff.setFullName("Maria");
        staff.setRole(role);
        return staff;
    }
}
