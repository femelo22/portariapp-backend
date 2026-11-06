package br.com.lfmelo.dto.deliveryevent;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.DeliveryStatus;

public record DeliveryEventDto(
        UUID id,
        UUID condominiumId,
        UUID deliveryId,
        long sequenceNumber,
        DeliveryStatus previousStatus,
        DeliveryStatus status,
        UUID staffId,
        Instant occurredAt,
        String notes) {
}
