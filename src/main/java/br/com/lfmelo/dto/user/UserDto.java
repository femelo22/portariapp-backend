package br.com.lfmelo.dto.user;

import java.time.Instant;
import java.util.UUID;

public record UserDto(UUID id, String email, boolean active, Instant createdAt) {
}
