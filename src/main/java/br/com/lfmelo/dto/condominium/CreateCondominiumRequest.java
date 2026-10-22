package br.com.lfmelo.dto.condominium;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCondominiumRequest(@NotBlank @Size(max = 150) String name) {
}
