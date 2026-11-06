package br.com.lfmelo.dto.staffmember;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.StaffRole;

public record StaffMemberDto(
        UUID id,
        UUID condominiumId,
        UUID userId,
        String fullName,
        boolean active,
        Instant createdAt,
        StaffRole role) {
}
