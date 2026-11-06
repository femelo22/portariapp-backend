package br.com.lfmelo.entities;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_events")
public class DeliveryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private DeliveryStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffMember staff;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(columnDefinition = "text")
    private String notes;

    public DeliveryEvent() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Condominium getCondominium() {
        return condominium;
    }

    public void setCondominium(Condominium condominium) {
        this.condominium = condominium;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public DeliveryStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(DeliveryStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public StaffMember getStaff() {
        return staff;
    }

    public void setStaff(StaffMember staff) {
        this.staff = staff;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
