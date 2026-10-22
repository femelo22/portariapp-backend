package br.com.lfmelo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUnitRequest(@NotBlank @Size(max = 30) String identifier) {
}
