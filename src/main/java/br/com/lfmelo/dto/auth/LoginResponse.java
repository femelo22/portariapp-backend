package br.com.lfmelo.dto.auth;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UUID staffMemberId,
        UUID condominiumId,
        String fullName,
        String email) {
}
