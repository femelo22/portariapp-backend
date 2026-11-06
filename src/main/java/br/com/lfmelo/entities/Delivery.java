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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_resident_id", nullable = false)
    private Resident recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by_staff_id", nullable = false)
    private StaffMember receivedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.RECEIVED;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    @Column(name = "picked_up_at")
    private Instant pickedUpAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "picked_up_by_staff_id")
    private StaffMember pickedUpBy;

    @Column(name = "picked_up_by_name", length = 150)
    private String pickedUpByName;

    @Column(columnDefinition = "text")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Delivery() {
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

    public Resident getRecipient() {
        return recipient;
    }

    public void setRecipient(Resident recipient) {
        this.recipient = recipient;
    }

    public StaffMember getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(StaffMember receivedBy) {
        this.receivedBy = receivedBy;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public StaffMember getPickedUpBy() {
        return pickedUpBy;
    }

    public void setPickedUpBy(StaffMember pickedUpBy) {
        this.pickedUpBy = pickedUpBy;
    }

    public String getPickedUpByName() {
        return pickedUpByName;
    }

    public void setPickedUpByName(String pickedUpByName) {
        this.pickedUpByName = pickedUpByName;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
