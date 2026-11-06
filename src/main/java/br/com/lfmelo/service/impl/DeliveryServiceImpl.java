package br.com.lfmelo.service.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.delivery.CreateDeliveryRequest;
import br.com.lfmelo.dto.delivery.DeliveryDto;
import br.com.lfmelo.dto.delivery.PickupDeliveryRequest;
import br.com.lfmelo.dto.deliveryevent.DeliveryEventDto;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.DeliveryEvent;
import br.com.lfmelo.entities.Resident;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.enums.DeliveryStatus;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.DeliveryEventMapper;
import br.com.lfmelo.mapper.DeliveryMapper;
import br.com.lfmelo.repository.CondominiumRepository;
import br.com.lfmelo.repository.DeliveryEventRepository;
import br.com.lfmelo.repository.DeliveryRepository;
import br.com.lfmelo.repository.ResidentRepository;
import br.com.lfmelo.security.CurrentStaffResolver;
import br.com.lfmelo.service.DeliveryService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository eventRepository;
    private final CondominiumRepository condominiumRepository;
    private final ResidentRepository residentRepository;
    private final CurrentStaffResolver staffResolver;
    private final Clock clock;

    public DeliveryServiceImpl(
            DeliveryRepository deliveryRepository,
            DeliveryEventRepository eventRepository,
            CondominiumRepository condominiumRepository,
            ResidentRepository residentRepository,
            CurrentStaffResolver staffResolver,
            Clock clock) {
        this.deliveryRepository = deliveryRepository;
        this.eventRepository = eventRepository;
        this.condominiumRepository = condominiumRepository;
        this.residentRepository = residentRepository;
        this.staffResolver = staffResolver;
        this.clock = clock;
    }

    @Override
    @Transactional
    public DeliveryDto create(UUID condominiumId, CreateDeliveryRequest request) {
        //Pega o staff pelo token
        StaffMember staff = staffResolver.requireActive(condominiumId);

        Condominium condominium = condominiumRepository
                .findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));

        Resident resident = residentRepository
                .findByIdAndCondominiumId(request.recipientResidentId(), condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Resident not found: " + request.recipientResidentId()));

        if (!resident.isActive()) {
            throw new BadRequestException("Recipient resident is inactive");
        }

        Delivery delivery = new Delivery();
        delivery.setCondominium(condominium);
        delivery.setRecipient(resident);
        delivery.setReceivedBy(staff);
        delivery.setStatus(DeliveryStatus.RECEIVED);
        delivery.setDescription(request.description());
        delivery.setNotes(request.notes());
        deliveryRepository.saveAndFlush(delivery);
        saveEvent(delivery, staff, 1, null, DeliveryStatus.RECEIVED, delivery.getReceivedAt(), request.notes());
        return DeliveryMapper.toDto(delivery);
    }

    @Override
    public List<DeliveryDto> findAll(UUID condominiumId, DeliveryStatus status, UUID recipientResidentId) {
        if (!condominiumRepository.existsById(condominiumId)) {
            throw new ResourceNotFoundException("Condominium not found: " + condominiumId);
        }
        Specification<Delivery> filters = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("condominium").get("id"), condominiumId));
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (recipientResidentId != null) {
                predicates.add(builder.equal(root.get("recipient").get("id"), recipientResidentId));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Sort sort = Sort.by(Sort.Order.desc("receivedAt"), Sort.Order.asc("id"));
        return deliveryRepository.findAll(filters, sort).stream().map(DeliveryMapper::toDto).toList();
    }

    @Override
    public DeliveryDto findById(UUID condominiumId, UUID deliveryId) {
        return DeliveryMapper.toDto(getOrThrow(condominiumId, deliveryId));
    }

    @Override
    public List<DeliveryEventDto> findEvents(UUID condominiumId, UUID deliveryId) {
        getOrThrow(condominiumId, deliveryId);
        return eventRepository.findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(condominiumId, deliveryId)
                .stream().map(DeliveryEventMapper::toDto).toList();
    }

    @Override
    @Transactional
    public DeliveryDto pickup(UUID condominiumId, UUID deliveryId, PickupDeliveryRequest request) {
        return changeStatus(condominiumId, deliveryId, DeliveryStatus.PICKED_UP, request.pickedUpByName(), request.notes());
    }

    @Override
    @Transactional
    public DeliveryDto transition(UUID condominiumId, UUID deliveryId, DeliveryStatus status, String notes) {
        if (status != DeliveryStatus.RETURNED && status != DeliveryStatus.LOST && status != DeliveryStatus.CANCELLED) {
            throw new BadRequestException("Unsupported delivery transition");
        }
        return changeStatus(condominiumId, deliveryId, status, null, notes);
    }

    private DeliveryDto changeStatus(
            UUID condominiumId, UUID deliveryId, DeliveryStatus status, String pickedUpByName, String notes) {
        StaffMember staff = staffResolver.requireActive(condominiumId);
        Delivery delivery = deliveryRepository.findByIdAndCondominiumIdForUpdate(deliveryId, condominiumId)
                .orElseThrow(() -> notFound(deliveryId));
        if (delivery.getStatus() != DeliveryStatus.RECEIVED) {
            throw new ConflictException("Delivery is already in a final status");
        }
        List<DeliveryEvent> events =
                eventRepository.findByCondominiumIdAndDeliveryIdOrderBySequenceNumberAsc(condominiumId, deliveryId);
        if (events.isEmpty() || events.get(events.size() - 1).getStatus() != delivery.getStatus()) {
            throw new ConflictException("Delivery status does not match its event history");
        }
        long sequenceNumber = events.get(events.size() - 1).getSequenceNumber() + 1;
        Instant occurredAt = clock.instant();
        delivery.setStatus(status);
        if (status == DeliveryStatus.PICKED_UP) {
            delivery.setPickedUpAt(occurredAt);
            delivery.setPickedUpBy(staff);
            delivery.setPickedUpByName(pickedUpByName);
        }
        deliveryRepository.saveAndFlush(delivery);
        saveEvent(delivery, staff, sequenceNumber, DeliveryStatus.RECEIVED, status, occurredAt, notes);
        return DeliveryMapper.toDto(delivery);
    }

    private void saveEvent(
            Delivery delivery, StaffMember staff, long sequenceNumber, DeliveryStatus previousStatus,
            DeliveryStatus status, Instant occurredAt, String notes) {
        DeliveryEvent event = new DeliveryEvent();
        event.setCondominium(delivery.getCondominium());
        event.setDelivery(delivery);
        event.setSequenceNumber(sequenceNumber);
        event.setPreviousStatus(previousStatus);
        event.setStatus(status);
        event.setStaff(staff);
        event.setOccurredAt(occurredAt);
        event.setNotes(notes);
        eventRepository.save(event);
    }

    private Delivery getOrThrow(UUID condominiumId, UUID deliveryId) {
        return deliveryRepository.findByIdAndCondominiumId(deliveryId, condominiumId)
                .orElseThrow(() -> notFound(deliveryId));
    }

    private ResourceNotFoundException notFound(UUID deliveryId) {
        return new ResourceNotFoundException("Delivery not found: " + deliveryId);
    }
}
