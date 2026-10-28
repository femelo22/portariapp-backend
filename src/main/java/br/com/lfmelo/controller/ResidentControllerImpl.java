package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.resident.CreateResidentRequest;
import br.com.lfmelo.dto.resident.ResidentDto;
import br.com.lfmelo.dto.resident.UpdateResidentRequest;
import br.com.lfmelo.service.ResidentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResidentControllerImpl implements ResidentController {

    private final ResidentService residentService;

    public ResidentControllerImpl(ResidentService residentService) {
        this.residentService = residentService;
    }

    @Override
    public ResponseEntity<List<ResidentDto>> findAll(UUID condominiumId) {
        return ResponseEntity.ok(residentService.findAll(condominiumId));
    }

    @Override
    public ResponseEntity<ResidentDto> findById(UUID condominiumId, UUID id) {
        return ResponseEntity.ok(residentService.findById(condominiumId, id));
    }

    @Override
    public ResponseEntity<ResidentDto> create(UUID condominiumId, CreateResidentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(residentService.create(condominiumId, request));
    }

    @Override
    public ResponseEntity<ResidentDto> update(
            UUID condominiumId, UUID id, UpdateResidentRequest request) {
        return ResponseEntity.ok(residentService.update(condominiumId, id, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID condominiumId, UUID id) {
        residentService.delete(condominiumId, id);
        return ResponseEntity.noContent().build();
    }
}
