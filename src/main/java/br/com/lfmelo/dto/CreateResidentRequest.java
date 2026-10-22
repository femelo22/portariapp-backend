package br.com.lfmelo.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateResidentRequest(
        @NotNull UUID condominiumId,
        @NotNull UUID unitId,
        UUID userId,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phone) {
}
