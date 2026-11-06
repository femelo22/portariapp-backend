package br.com.lfmelo.dto.delivery;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CreateDeliveryRequest(
        @NotNull UUID recipientResidentId,
        String description,
        String notes) {
}
