package br.com.lfmelo.entities;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.enums.EvidenceContentTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "delivery_evidences")
public class DeliveryEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "condominium_id", nullable = false)
    private Condominium condominium;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_staff_id", nullable = false)
    private StaffMember uploadedBy;

    @Column(name = "storage_file_key", nullable = false, length = 500)
    private String storageFileKey;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Convert(converter = EvidenceContentTypeConverter.class)
    @Column(name = "content_type", nullable = false, length = 100)
    private EvidenceContentType contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DeliveryEvidence() {
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

    public StaffMember getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(StaffMember uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public String getStorageFileKey() {
        return storageFileKey;
    }

    public void setStorageFileKey(String storageFileKey) {
        this.storageFileKey = storageFileKey;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public EvidenceContentType getContentType() {
        return contentType;
    }

    public void setContentType(EvidenceContentType contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
