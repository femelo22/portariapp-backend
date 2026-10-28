package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.unit.CreateUnitRequest;
import br.com.lfmelo.dto.unit.UnitDto;
import br.com.lfmelo.dto.unit.UpdateUnitRequest;
import br.com.lfmelo.service.UnitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnitControllerImpl implements UnitController {

    private final UnitService unitService;

    public UnitControllerImpl(UnitService unitService) {
        this.unitService = unitService;
    }

    @Override
    public ResponseEntity<List<UnitDto>> findAll(UUID condominiumId) {
        return ResponseEntity.ok(unitService.findAll(condominiumId));
    }

    @Override
    public ResponseEntity<UnitDto> findById(UUID condominiumId, UUID id) {
        return ResponseEntity.ok(unitService.findById(condominiumId, id));
    }

    @Override
    public ResponseEntity<UnitDto> create(UUID condominiumId, CreateUnitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(unitService.create(condominiumId, request));
    }

    @Override
    public ResponseEntity<UnitDto> update(UUID condominiumId, UUID id, UpdateUnitRequest request) {
        return ResponseEntity.ok(unitService.update(condominiumId, id, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID condominiumId, UUID id) {
        unitService.delete(condominiumId, id);
        return ResponseEntity.noContent().build();
    }
}
