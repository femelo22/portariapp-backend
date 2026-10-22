package br.com.lfmelo.dto.unit;

import java.time.Instant;
import java.util.UUID;

public record UnitDto(UUID id, UUID condominiumId, String identifier, Instant createdAt) {
}
