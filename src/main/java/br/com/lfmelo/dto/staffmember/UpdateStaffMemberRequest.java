package br.com.lfmelo.dto.staffmember;

import br.com.lfmelo.enums.StaffRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStaffMemberRequest(
        @NotBlank @Size(max = 150) String fullName,
        boolean active,
        @NotNull StaffRole role) {
}
