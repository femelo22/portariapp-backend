package br.com.lfmelo.dto.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PickupDeliveryRequest(
        @NotBlank @Size(max = 150) String pickedUpByName,
        String notes) {
}
