package br.com.lfmelo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import br.com.lfmelo.dto.delivery.CreateDeliveryRequest;
import br.com.lfmelo.dto.delivery.PickupDeliveryRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.DeliveryEvent;
import br.com.lfmelo.entities.Resident;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.enums.DeliveryStatus;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.repository.CondominiumRepository;
import br.com.lfmelo.repository.DeliveryEventRepository;
import br.com.lfmelo.repository.DeliveryRepository;
import br.com.lfmelo.repository.ResidentRepository;
import br.com.lfmelo.security.CurrentStaffResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");
    private final UUID condominiumId = UUID.randomUUID();
    private final UUID deliveryId = UUID.randomUUID();
    private final UUID residentId = UUID.randomUUID();
    private final UUID staffId = UUID.randomUUID();

    @Mock private DeliveryRepository deliveries;
    @Mock private DeliveryEventRepository events;
    @Mock private CondominiumRepository condominiums;
    @Mock private ResidentRepository residents;
    @Mock private CurrentStaffResolver staffResolver;

    private DeliveryServiceImpl service;
    private Condominium condominium;
    private Resident resident;
    private StaffMember staff;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        service = new DeliveryServiceImpl(
                deliveries, events, condominiums, residents, staffResolver, Clock.fixed(NOW, ZoneOffset.UTC));
        condominium = new Condominium();
        condominium.setId(condominiumId);
        resident = new Resident();
        resident.setId(residentId);
        resident.setCondominium(condominium);
        staff = new StaffMember();
        staff.setId(staffId);
        staff.setCondominium(condominium);
        delivery = new Delivery();
        delivery.setId(deliveryId);
        delivery.setCondominium(condominium);
        delivery.setRecipient(resident);
        delivery.setReceivedBy(staff);
        delivery.setReceivedAt(NOW);
    }

    @Test
    void shouldCreateReceivedDeliveryAndInitialEvent() {
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(condominiums.findById(condominiumId)).thenReturn(Optional.of(condominium));
        when(residents.findByIdAndCondominiumId(residentId, condominiumId)).thenReturn(Optional.of(resident));
        when(deliveries.saveAndFlush(any())).thenAnswer(invocation -> {
            Delivery saved = invocation.getArgument(0);
            saved.setId(deliveryId);
            saved.setReceivedAt(NOW);
            return saved;
        });

        var result = service.create(condominiumId, new CreateDeliveryRequest(residentId, "Box", "Reception"));

        ArgumentCaptor<DeliveryEvent> captor = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(events).save(captor.capture());
        DeliveryEvent event = captor.getValue();
        assertThat(result.status()).isEqualTo(DeliveryStatus.RECEIVED);
        assertThat(result.receivedByStaffId()).isEqualTo(staffId);
        assertThat(event.getSequenceNumber()).isEqualTo(1);
        assertThat(event.getPreviousStatus()).isNull();
        assertThat(event.getStatus()).isEqualTo(DeliveryStatus.RECEIVED);
        assertThat(event.getStaff()).isSameAs(staff);
        assertThat(event.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void shouldRejectInactiveResident() {
        resident.setActive(false);
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(condominiums.findById(condominiumId)).thenReturn(Optional.of(condominium));
        when(residents.findByIdAndCondominiumId(residentId, condominiumId)).thenReturn(Optional.of(resident));

        assertThatThrownBy(() -> service.create(condominiumId, new CreateDeliveryRequest(residentId, null, null)))
                .isInstanceOf(BadRequestException.class);
        verify(deliveries, never()).saveAndFlush(any());
        verify(events, never()).save(any());
    }

    @Test
    void shouldRejectResidentOutsideCondominium() {
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(condominiums.findById(condominiumId)).thenReturn(Optional.of(condominium));
        when(residents.findByIdAndCondominiumId(residentId, condominiumId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(condominiumId, new CreateDeliveryRequest(residentId, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(deliveries, never()).saveAndFlush(any());
        verify(events, never()).save(any());
    }

    @Test
    void shouldRecordPickupWithSameTimestampAndStaffInEvent() {
        prepareTransition();

        var result = service.pickup(condominiumId, deliveryId, new PickupDeliveryRequest("Maria", "Checked ID"));

        ArgumentCaptor<DeliveryEvent> captor = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(events).save(captor.capture());
        DeliveryEvent event = captor.getValue();
        assertThat(result.status()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(result.pickedUpAt()).isEqualTo(NOW);
        assertThat(result.pickedUpByStaffId()).isEqualTo(staffId);
        assertThat(result.pickedUpByName()).isEqualTo("Maria");
        assertThat(event.getSequenceNumber()).isEqualTo(2);
        assertThat(event.getPreviousStatus()).isEqualTo(DeliveryStatus.RECEIVED);
        assertThat(event.getStatus()).isEqualTo(DeliveryStatus.PICKED_UP);
        assertThat(event.getOccurredAt()).isEqualTo(NOW);
        assertThat(event.getNotes()).isEqualTo("Checked ID");
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class, names = {"RETURNED", "LOST", "CANCELLED"})
    void shouldRecordFinalStatusAndEvent(DeliveryStatus status) {
        prepareTransition();

        var result = service.transition(condominiumId, deliveryId, status, "Checked");

        ArgumentCaptor<DeliveryEvent> captor = ArgumentCaptor.forClass(DeliveryEvent.class);
        verify(events).save(captor.capture());
        assertThat(result.status()).isEqualTo(status);
        assertThat(captor.getValue().getStatus()).isEqualTo(status);
        assertThat(captor.getValue().getStaff()).isSameAs(staff);
        assertThat(captor.getValue().getNotes()).isEqualTo("Checked");
    }

    @Test
    void shouldRejectTransitionFromFinalStatus() {
        delivery.setStatus(DeliveryStatus.PICKED_UP);
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(deliveries.findByIdAndCondominiumIdForUpdate(deliveryId, condominiumId))
                .thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> service.transition(condominiumId, deliveryId, DeliveryStatus.LOST, null))
                .isInstanceOf(ConflictException.class);
        verify(events, never()).save(any());
    }

    @Test
    void shouldRejectMissingHistory() {
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(deliveries.findByIdAndCondominiumIdForUpdate(deliveryId, condominiumId))
                .thenReturn(Optional.of(delivery));
        when(events.findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(condominiumId, deliveryId))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.transition(condominiumId, deliveryId, DeliveryStatus.CANCELLED, null))
                .isInstanceOf(ConflictException.class);
        verify(deliveries, never()).saveAndFlush(any());
    }

    @Test
    void shouldRejectPickupThroughGenericTransition() {
        assertThatThrownBy(() -> service.transition(condominiumId, deliveryId, DeliveryStatus.PICKED_UP, null))
                .isInstanceOf(BadRequestException.class);
        verify(deliveries, never()).saveAndFlush(any());
        verify(events, never()).save(any());
    }

    @Test
    void shouldNotListEventsOfMissingDelivery() {
        when(deliveries.findByIdAndCondominiumId(deliveryId, condominiumId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findEvents(condominiumId, deliveryId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(events, never()).findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(any(), any());
    }

    @Test
    void shouldListEventsInRepositoryOrder() {
        DeliveryEvent initial = new DeliveryEvent();
        initial.setCondominium(condominium);
        initial.setDelivery(delivery);
        initial.setStaff(staff);
        initial.setSequenceNumber(1);
        initial.setStatus(DeliveryStatus.RECEIVED);
        DeliveryEvent terminal = new DeliveryEvent();
        terminal.setCondominium(condominium);
        terminal.setDelivery(delivery);
        terminal.setStaff(staff);
        terminal.setSequenceNumber(2);
        terminal.setPreviousStatus(DeliveryStatus.RECEIVED);
        terminal.setStatus(DeliveryStatus.LOST);
        when(deliveries.findByIdAndCondominiumId(deliveryId, condominiumId)).thenReturn(Optional.of(delivery));
        when(events.findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(condominiumId, deliveryId))
                .thenReturn(List.of(initial, terminal));

        var result = service.findEvents(condominiumId, deliveryId);

        assertThat(result).extracting(event -> event.status())
                .containsExactly(DeliveryStatus.RECEIVED, DeliveryStatus.LOST);
    }

    private void prepareTransition() {
        DeliveryEvent initial = new DeliveryEvent();
        initial.setSequenceNumber(1);
        initial.setStatus(DeliveryStatus.RECEIVED);
        when(staffResolver.requireActive(condominiumId)).thenReturn(staff);
        when(deliveries.findByIdAndCondominiumIdForUpdate(deliveryId, condominiumId))
                .thenReturn(Optional.of(delivery));
        when(events.findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(condominiumId, deliveryId))
                .thenReturn(List.of(initial));
    }
}
