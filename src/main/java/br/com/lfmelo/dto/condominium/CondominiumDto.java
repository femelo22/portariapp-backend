package br.com.lfmelo.dto.condominium;

import java.time.Instant;
import java.util.UUID;

public record CondominiumDto(UUID id, String name, Instant createdAt) {
}
