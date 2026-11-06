package br.com.lfmelo.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.DeliveryEvent;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.enums.DeliveryStatus;
import org.junit.jupiter.api.Test;

class DeliveryEventMapperTest {

    @Test
    void mapsEveryFieldOfTransitionEvent() {
        UUID eventId = UUID.randomUUID();
        UUID condominiumId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-01-02T03:04:05Z");
        DeliveryEvent event = event(eventId, condominiumId, deliveryId, staffId);
        event.setSequenceNumber(2);
        event.setPreviousStatus(DeliveryStatus.RECEIVED);
        event.setStatus(DeliveryStatus.PICKED_UP);
        event.setOccurredAt(occurredAt);
        event.setNotes("Retirada");

        assertEquals(new DeliveryEventDto(eventId, condominiumId, deliveryId, 2,
                DeliveryStatus.RECEIVED, DeliveryStatus.PICKED_UP, staffId, occurredAt, "Retirada"),
                DeliveryEventMapper.toDto(event));
    }

    @Test
    void mapsNullableFieldsOfInitialEvent() {
        UUID eventId = UUID.randomUUID();
        UUID condominiumId = UUID.randomUUID();
        UUID deliveryId = UUID.randomUUID();
        UUID staffId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-01-02T03:04:05Z");
        DeliveryEvent event = event(eventId, condominiumId, deliveryId, staffId);
        event.setSequenceNumber(1);
        event.setStatus(DeliveryStatus.RECEIVED);
        event.setOccurredAt(occurredAt);

        assertEquals(new DeliveryEventDto(eventId, condominiumId, deliveryId, 1, null,
                DeliveryStatus.RECEIVED, staffId, occurredAt, null), DeliveryEventMapper.toDto(event));
    }

    private DeliveryEvent event(UUID eventId, UUID condominiumId, UUID deliveryId, UUID staffId) {
        Condominium condominium = new Condominium();
        condominium.setId(condominiumId);
        Delivery delivery = new TestDelivery();
        delivery.setId(deliveryId);
        StaffMember staff = new StaffMember();
        staff.setId(staffId);
        DeliveryEvent event = new DeliveryEvent();
        event.setId(eventId);
        event.setCondominium(condominium);
        event.setDelivery(delivery);
        event.setStaff(staff);
        return event;
    }

    private static class TestDelivery extends Delivery {
    }
}
