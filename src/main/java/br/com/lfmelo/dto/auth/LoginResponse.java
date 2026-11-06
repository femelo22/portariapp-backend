package br.com.lfmelo.dto.auth;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.StaffRole;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UUID staffMemberId,
        UUID condominiumId,
        String fullName,
        String email,
        StaffRole role) {
}
