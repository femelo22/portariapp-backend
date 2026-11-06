package br.com.lfmelo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.enums.StaffRole;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class StaffMemberRoleMapperTest {

    @ParameterizedTest
    @EnumSource(StaffRole.class)
    void shouldMapRoleAndExistingFields(StaffRole role) {
        Condominium condominium = new Condominium();
        condominium.setId(UUID.randomUUID());
        User user = new User();
        user.setId(UUID.randomUUID());
        StaffMember staff = new StaffMember();
        staff.setId(UUID.randomUUID());
        staff.setCondominium(condominium);
        staff.setUser(user);
        staff.setFullName("Maria");
        staff.setActive(false);
        staff.setCreatedAt(Instant.parse("2026-01-02T03:04:05Z"));
        staff.setRole(role);

        var dto = StaffMemberMapper.toDto(staff);

        assertThat(dto.id()).isEqualTo(staff.getId());
        assertThat(dto.condominiumId()).isEqualTo(condominium.getId());
        assertThat(dto.userId()).isEqualTo(user.getId());
        assertThat(dto.fullName()).isEqualTo("Maria");
        assertThat(dto.active()).isFalse();
        assertThat(dto.createdAt()).isEqualTo(staff.getCreatedAt());
        assertThat(dto.role()).isEqualTo(role);

        staff.setUser(null);
        assertThat(StaffMemberMapper.toDto(staff).userId()).isNull();
        assertThat(StaffMemberMapper.toDto(staff).role()).isEqualTo(role);
    }
}
