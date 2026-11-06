package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.delivery.CreateDeliveryRequest;
import br.com.lfmelo.dto.delivery.DeliveryDto;
import br.com.lfmelo.dto.delivery.PickupDeliveryRequest;
import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.enums.DeliveryStatus;

public interface DeliveryService {

    DeliveryDto create(UUID condominiumId, CreateDeliveryRequest request);

    List<DeliveryDto> findAll(UUID condominiumId, DeliveryStatus status, UUID recipientResidentId);

    DeliveryDto findById(UUID condominiumId, UUID deliveryId);

    List<DeliveryEventDto> findEvents(UUID condominiumId, UUID deliveryId);

    DeliveryDto pickup(UUID condominiumId, UUID deliveryId, PickupDeliveryRequest request);

    DeliveryDto transition(UUID condominiumId, UUID deliveryId, DeliveryStatus status, String notes);
}
