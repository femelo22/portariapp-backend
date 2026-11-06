package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.delivery.CreateDeliveryRequest;
import br.com.lfmelo.dto.delivery.DeliveryDto;
import br.com.lfmelo.dto.delivery.DeliveryTransitionRequest;
import br.com.lfmelo.dto.delivery.PickupDeliveryRequest;
import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.enums.DeliveryStatus;
import br.com.lfmelo.service.DeliveryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeliveryControllerImpl implements DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryControllerImpl(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @Override
    public ResponseEntity<DeliveryDto> create(UUID condominiumId, CreateDeliveryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.create(condominiumId, request));
    }

    @Override
    public ResponseEntity<List<DeliveryDto>> findAll(
            UUID condominiumId, DeliveryStatus status, UUID recipientResidentId) {
        return ResponseEntity.ok(deliveryService.findAll(condominiumId, status, recipientResidentId));
    }

    @Override
    public ResponseEntity<DeliveryDto> findById(UUID condominiumId, UUID deliveryId) {
        return ResponseEntity.ok(deliveryService.findById(condominiumId, deliveryId));
    }

    @Override
    public ResponseEntity<List<DeliveryEventDto>> findEvents(UUID condominiumId, UUID deliveryId) {
        return ResponseEntity.ok(deliveryService.findEvents(condominiumId, deliveryId));
    }

    @Override
    public ResponseEntity<DeliveryDto> pickup(UUID condominiumId, UUID deliveryId, PickupDeliveryRequest request) {
        return ResponseEntity.ok(deliveryService.pickup(condominiumId, deliveryId, request));
    }

    @Override
    public ResponseEntity<DeliveryDto> returnDelivery(
            UUID condominiumId, UUID deliveryId, DeliveryTransitionRequest request) {
        return transition(condominiumId, deliveryId, DeliveryStatus.RETURNED, request);
    }

    @Override
    public ResponseEntity<DeliveryDto> markLost(
            UUID condominiumId, UUID deliveryId, DeliveryTransitionRequest request) {
        return transition(condominiumId, deliveryId, DeliveryStatus.LOST, request);
    }

    @Override
    public ResponseEntity<DeliveryDto> cancel(
            UUID condominiumId, UUID deliveryId, DeliveryTransitionRequest request) {
        return transition(condominiumId, deliveryId, DeliveryStatus.CANCELLED, request);
    }

    private ResponseEntity<DeliveryDto> transition(
            UUID condominiumId, UUID deliveryId, DeliveryStatus status, DeliveryTransitionRequest request) {
        return ResponseEntity.ok(deliveryService.transition(
                condominiumId, deliveryId, status, request == null ? null : request.notes()));
    }
}
