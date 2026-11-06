package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.staffmember.StaffMemberDto;
import br.com.lfmelo.entities.StaffMember;

/** Pure, stateless conversion between {@link StaffMember} and {@link StaffMemberDto}. */
public final class StaffMemberMapper {

    private StaffMemberMapper() {
    }

    public static StaffMemberDto toDto(StaffMember staffMember) {
        return new StaffMemberDto(
                staffMember.getId(),
                staffMember.getCondominium().getId(),
                staffMember.getUser() != null ? staffMember.getUser().getId() : null,
                staffMember.getFullName(),
                staffMember.isActive(),
                staffMember.getCreatedAt(),
                staffMember.getRole());
    }
}
