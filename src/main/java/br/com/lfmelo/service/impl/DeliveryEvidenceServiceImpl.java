package br.com.lfmelo.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceContent;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceDto;
import br.com.lfmelo.entities.Delivery;
import br.com.lfmelo.entities.DeliveryEvidence;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.enums.EvidenceContentType;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.repository.DeliveryEvidenceRepository;
import br.com.lfmelo.repository.DeliveryRepository;
import br.com.lfmelo.security.CurrentStaffResolver;
import br.com.lfmelo.service.DeliveryEvidenceService;
import br.com.lfmelo.storage.EvidenceStorage;
import br.com.lfmelo.storage.EvidenceStorage.StoredEvidence;
import br.com.lfmelo.storage.EvidenceStorageException;
import br.com.lfmelo.storage.EvidenceSizeExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional(readOnly = true)
public class DeliveryEvidenceServiceImpl implements DeliveryEvidenceService {
    private static final Logger log = LoggerFactory.getLogger(DeliveryEvidenceServiceImpl.class);
    private final DeliveryEvidenceRepository evidenceRepository;
    private final DeliveryRepository deliveryRepository;
    private final CurrentStaffResolver currentStaffResolver;
    private final EvidenceStorage storage;
    private final long maximumBytes;

    public DeliveryEvidenceServiceImpl(
            DeliveryEvidenceRepository evidenceRepository, DeliveryRepository deliveryRepository,
            CurrentStaffResolver currentStaffResolver, EvidenceStorage storage,
            @Value("${evidences.max-file-size-bytes:10485760}") long maximumBytes) {
        if (maximumBytes <= 0) {
            throw new IllegalArgumentException("Evidence maximum size must be positive");
        }
        this.evidenceRepository = evidenceRepository;
        this.deliveryRepository = deliveryRepository;
        this.currentStaffResolver = currentStaffResolver;
        this.storage = storage;
        this.maximumBytes = maximumBytes;
    }

    @Override
    @Transactional
    public DeliveryEvidenceDto upload(UUID condominiumId, UUID deliveryId, MultipartFile file) {
        StaffMember staff = currentStaffResolver.requireActive(condominiumId);
        Delivery delivery = requireDelivery(condominiumId, deliveryId);
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Evidence file must not be empty");
        }
        if (file.getSize() > maximumBytes) {
            throw new EvidenceSizeExceededException();
        }
        EvidenceContentType declaredType;
        try {
            declaredType = EvidenceContentType.fromValue(file.getContentType());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported evidence content type");
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Evidence upload requires an active transaction");
        }
        StoredEvidence stored = null;
        try (InputStream input = file.getInputStream()) {
            stored = storage.store(input, declaredType, maximumBytes);
        } catch (IOException ex) {
            if (stored != null) {
                cleanup(stored.key());
            }
            throw new EvidenceStorageException("Could not read evidence upload", ex);
        } catch (RuntimeException ex) {
            if (stored != null) {
                cleanup(stored.key());
            }
            throw ex;
        }
        final StoredEvidence savedFile = stored;
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        cleanup(savedFile.key());
                    } else if (status == STATUS_UNKNOWN) {
                        log.error("Evidence transaction outcome is unknown; file requires reconciliation; key={}",
                                savedFile.key());
                    }
                }
            });
        } catch (RuntimeException ex) {
            cleanup(savedFile.key());
            throw ex;
        }

        DeliveryEvidence evidence = new DeliveryEvidence();
        evidence.setCondominium(delivery.getCondominium());
        evidence.setDelivery(delivery);
        evidence.setUploadedBy(staff);
        evidence.setStorageFileKey(stored.key());
        evidence.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        evidence.setContentType(stored.contentType());
        evidence.setSizeBytes(stored.sizeBytes());
        return toDto(evidenceRepository.saveAndFlush(evidence));
    }

    @Override
    public List<DeliveryEvidenceDto> findAll(UUID condominiumId, UUID deliveryId) {
        requireDelivery(condominiumId, deliveryId);
        return evidenceRepository.findByCondominiumIdAndDeliveryIdOrderByCreatedAtDesc(condominiumId, deliveryId)
                .stream().map(DeliveryEvidenceServiceImpl::toDto).toList();
    }

    @Override
    public DeliveryEvidenceContent getContent(UUID condominiumId, UUID deliveryId, UUID evidenceId) {
        requireDelivery(condominiumId, deliveryId);
        DeliveryEvidence evidence = evidenceRepository.findByIdAndCondominiumIdAndDeliveryId(
                evidenceId, condominiumId, deliveryId).orElseThrow(() -> new ResourceNotFoundException("Evidence not found"));
        return new DeliveryEvidenceContent(storage.load(evidence.getStorageFileKey()),
                evidence.getContentType(), evidence.getSizeBytes());
    }

    private Delivery requireDelivery(UUID condominiumId, UUID deliveryId) {
        return deliveryRepository.findByIdAndCondominiumId(deliveryId, condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery not found"));
    }

    private void cleanup(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException ex) {
            log.error("Failed to clean up evidence file after transaction failure; key={}", key, ex);
        }
    }

    private static String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String base = filename.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
        if (base.isBlank() || base.equals(".") || base.equals("..")) {
            return null;
        }
        return base.length() > 255 ? base.substring(0, 255) : base;
    }

    private static DeliveryEvidenceDto toDto(DeliveryEvidence evidence) {
        return new DeliveryEvidenceDto(evidence.getId(), evidence.getCondominium().getId(),
                evidence.getDelivery().getId(), evidence.getUploadedBy().getId(), evidence.getOriginalFilename(),
                evidence.getContentType(), evidence.getSizeBytes(), evidence.getCreatedAt());
    }
}
