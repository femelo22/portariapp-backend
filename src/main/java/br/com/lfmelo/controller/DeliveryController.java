package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.delivery.CreateDeliveryRequest;
import br.com.lfmelo.dto.delivery.DeliveryDto;
import br.com.lfmelo.dto.delivery.DeliveryTransitionRequest;
import br.com.lfmelo.dto.delivery.PickupDeliveryRequest;
import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.enums.DeliveryStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RequestMapping("/api/v1/condominiums/{condominiumId}/deliveries")
public interface DeliveryController {

    @PostMapping
    ResponseEntity<DeliveryDto> create(
            @PathVariable UUID condominiumId, @Valid @RequestBody CreateDeliveryRequest request);

    @GetMapping
    ResponseEntity<List<DeliveryDto>> findAll(
            @PathVariable UUID condominiumId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) UUID recipientResidentId);

    @GetMapping("/{deliveryId}")
    ResponseEntity<DeliveryDto> findById(@PathVariable UUID condominiumId, @PathVariable UUID deliveryId);

    @GetMapping("/{deliveryId}/events")
    ResponseEntity<List<DeliveryEventDto>> findEvents(
            @PathVariable UUID condominiumId, @PathVariable UUID deliveryId);

    @PostMapping("/{deliveryId}/pickup")
    ResponseEntity<DeliveryDto> pickup(
            @PathVariable UUID condominiumId,
            @PathVariable UUID deliveryId,
            @Valid @RequestBody PickupDeliveryRequest request);

    @PostMapping("/{deliveryId}/return")
    ResponseEntity<DeliveryDto> returnDelivery(
            @PathVariable UUID condominiumId,
            @PathVariable UUID deliveryId,
            @RequestBody(required = false) DeliveryTransitionRequest request);

    @PostMapping("/{deliveryId}/lost")
    ResponseEntity<DeliveryDto> markLost(
            @PathVariable UUID condominiumId,
            @PathVariable UUID deliveryId,
            @RequestBody(required = false) DeliveryTransitionRequest request);

    @PostMapping("/{deliveryId}/cancel")
    ResponseEntity<DeliveryDto> cancel(
            @PathVariable UUID condominiumId,
            @PathVariable UUID deliveryId,
            @RequestBody(required = false) DeliveryTransitionRequest request);
}
