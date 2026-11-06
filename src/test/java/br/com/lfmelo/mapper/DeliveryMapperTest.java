package br.com.lfmelo.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.Resident;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.enums.DeliveryStatus;
import org.junit.jupiter.api.Test;

class DeliveryMapperTest {

    @Test
    void shouldMapCurrentStateIncludingPickup() {
        Condominium condominium = new Condominium();
        condominium.setId(UUID.randomUUID());
        Resident resident = new Resident();
        resident.setId(UUID.randomUUID());
        StaffMember receiver = new StaffMember();
        receiver.setId(UUID.randomUUID());
        StaffMember pickupStaff = new StaffMember();
        pickupStaff.setId(UUID.randomUUID());
        Delivery delivery = new Delivery();
        delivery.setId(UUID.randomUUID());
        delivery.setCondominium(condominium);
        delivery.setRecipient(resident);
        delivery.setReceivedBy(receiver);
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        delivery.setPickedUpAt(Instant.parse("2026-01-02T03:04:05Z"));
        delivery.setPickedUpBy(pickupStaff);
        delivery.setPickedUpByName("Maria");

        var result = DeliveryMapper.toDto(delivery);

        assertThat(result.id()).isEqualTo(delivery.getId());
        assertThat(result.condominiumId()).isEqualTo(condominium.getId());
        assertThat(result.recipientResidentId()).isEqualTo(resident.getId());
        assertThat(result.receivedByStaffId()).isEqualTo(receiver.getId());
        assertThat(result.pickedUpByStaffId()).isEqualTo(pickupStaff.getId());
        assertThat(result.pickedUpByName()).isEqualTo("Maria");
        assertThat(result.pickedUpAt()).isEqualTo(delivery.getPickedUpAt());
    }

    @Test
    void shouldMapDeliveryWithoutPickup() {
        Condominium condominium = new Condominium();
        Resident resident = new Resident();
        StaffMember receiver = new StaffMember();
        Delivery delivery = new Delivery();
        delivery.setCondominium(condominium);
        delivery.setRecipient(resident);
        delivery.setReceivedBy(receiver);

        var result = DeliveryMapper.toDto(delivery);

        assertThat(result.status()).isEqualTo(DeliveryStatus.RECEIVED);
        assertThat(result.pickedUpByStaffId()).isNull();
        assertThat(result.pickedUpByName()).isNull();
        assertThat(result.pickedUpAt()).isNull();
    }
}
