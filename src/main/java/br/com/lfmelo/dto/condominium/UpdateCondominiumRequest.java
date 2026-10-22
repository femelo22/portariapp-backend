package br.com.lfmelo.dto.condominium;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCondominiumRequest(@NotBlank @Size(max = 150) String name) {
}
