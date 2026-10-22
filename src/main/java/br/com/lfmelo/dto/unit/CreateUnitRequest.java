package br.com.lfmelo.dto.unit;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
        @NotNull UUID condominiumId, @NotBlank @Size(max = 30) String identifier) {
}
