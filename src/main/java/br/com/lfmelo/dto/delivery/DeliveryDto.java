package br.com.lfmelo.dto.delivery;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.DeliveryStatus;

public record DeliveryDto(
        UUID id,
        UUID condominiumId,
        UUID recipientResidentId,
        UUID receivedByStaffId,
        DeliveryStatus status,
        String description,
        Instant receivedAt,
        Instant pickedUpAt,
        UUID pickedUpByStaffId,
        String pickedUpByName,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
