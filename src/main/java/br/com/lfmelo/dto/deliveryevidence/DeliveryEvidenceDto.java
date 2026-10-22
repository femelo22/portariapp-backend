package br.com.lfmelo.dto.deliveryevidence;

import java.time.Instant;
import java.util.UUID;
import br.com.lfmelo.enums.EvidenceContentType;

public record DeliveryEvidenceDto(
        UUID id,
        UUID condominiumId,
        UUID deliveryId,
        UUID uploadedByStaffId,
        String storageFileKey,
        String originalFilename,
        EvidenceContentType contentType,
        long sizeBytes,
        Instant createdAt) {
}
