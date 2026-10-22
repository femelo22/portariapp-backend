package br.com.lfmelo.dto.staffmember;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateStaffMemberRequest(@NotBlank @Size(max = 150) String fullName, boolean active) {
}
