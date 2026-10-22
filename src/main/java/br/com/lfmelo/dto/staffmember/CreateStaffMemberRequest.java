package br.com.lfmelo.dto.staffmember;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffMemberRequest(
        @NotNull UUID condominiumId, UUID userId, @NotBlank @Size(max = 150) String fullName) {
}
