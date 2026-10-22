package br.com.lfmelo.dto.resident;

import java.time.Instant;
import java.util.UUID;

public record ResidentDto(
        UUID id,
        UUID condominiumId,
        UUID unitId,
        UUID userId,
        String fullName,
        String phone,
        boolean active,
        Instant createdAt) {
}
