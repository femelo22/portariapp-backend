package br.com.lfmelo.dto.staffmember;

import java.time.Instant;
import java.util.UUID;

public record StaffMemberDto(
        UUID id,
        UUID condominiumId,
        UUID userId,
        String fullName,
        boolean active,
        Instant createdAt) {
}
