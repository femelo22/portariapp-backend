package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceContent;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceDto;
import br.com.lfmelo.service.DeliveryEvidenceService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class DeliveryEvidenceControllerImpl implements DeliveryEvidenceController {
    private final DeliveryEvidenceService service;

    public DeliveryEvidenceControllerImpl(DeliveryEvidenceService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<DeliveryEvidenceDto> upload(UUID condominiumId, UUID deliveryId, MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(condominiumId, deliveryId, file));
    }

    @Override
    public ResponseEntity<List<DeliveryEvidenceDto>> findAll(UUID condominiumId, UUID deliveryId) {
        return ResponseEntity.ok(service.findAll(condominiumId, deliveryId));
    }

    @Override
    public ResponseEntity<Resource> getContent(UUID condominiumId, UUID deliveryId, UUID evidenceId) {
        DeliveryEvidenceContent content = service.getContent(condominiumId, deliveryId, evidenceId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(content.contentType().value()))
                .contentLength(content.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.resource());
    }
}
