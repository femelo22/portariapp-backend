package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.deliveryevidence.DeliveryEvidenceDto;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@RequestMapping("/api/v1/condominiums/{condominiumId}/deliveries/{deliveryId}/evidences")
public interface DeliveryEvidenceController {
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DeliveryEvidenceDto> upload(
            @PathVariable UUID condominiumId, @PathVariable UUID deliveryId, @RequestPart("file") MultipartFile file);

    @GetMapping
    ResponseEntity<List<DeliveryEvidenceDto>> findAll(@PathVariable UUID condominiumId, @PathVariable UUID deliveryId);

    @GetMapping("/{evidenceId}/content")
    ResponseEntity<Resource> getContent(
            @PathVariable UUID condominiumId, @PathVariable UUID deliveryId, @PathVariable UUID evidenceId);
}
