package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.entities.DeliveryEvent;

public final class DeliveryEventMapper {

    private DeliveryEventMapper() {
    }

    public static DeliveryEventDto toDto(DeliveryEvent event) {
        return new DeliveryEventDto(
                event.getId(),
                event.getCondominium().getId(),
                event.getDelivery().getId(),
                event.getSequenceNumber(),
                event.getPreviousStatus(),
                event.getStatus(),
                event.getStaff().getId(),
                event.getOccurredAt(),
                event.getNotes());
    }
}
