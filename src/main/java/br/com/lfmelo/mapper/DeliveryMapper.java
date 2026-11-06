package br.com.lfmelo.mapper;

import br.com.lfmelo.dto.delivery.DeliveryDto;
import br.com.lfmelo.entities.Delivery;

public final class DeliveryMapper {

    private DeliveryMapper() {
    }

    public static DeliveryDto toDto(Delivery delivery) {
        return new DeliveryDto(
                delivery.getId(),
                delivery.getCondominium().getId(),
                delivery.getRecipient().getId(),
                delivery.getReceivedBy().getId(),
                delivery.getStatus(),
                delivery.getDescription(),
                delivery.getReceivedAt(),
                delivery.getPickedUpAt(),
                delivery.getPickedUpBy() == null ? null : delivery.getPickedUpBy().getId(),
                delivery.getPickedUpByName(),
                delivery.getNotes(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt());
    }
}
