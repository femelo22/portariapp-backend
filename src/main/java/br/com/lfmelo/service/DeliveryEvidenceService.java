package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceContent;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceDto;
import org.springframework.web.multipart.MultipartFile;

public interface DeliveryEvidenceService {
    DeliveryEvidenceDto upload(UUID condominiumId, UUID deliveryId, MultipartFile file);

    List<DeliveryEvidenceDto> findAll(UUID condominiumId, UUID deliveryId);

    DeliveryEvidenceContent getContent(UUID condominiumId, UUID deliveryId, UUID evidenceId);
}
