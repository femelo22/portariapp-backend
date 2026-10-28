package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.condominium.CondominiumDto;
import br.com.lfmelo.dto.condominium.CreateCondominiumRequest;
import br.com.lfmelo.dto.condominium.UpdateCondominiumRequest;
import br.com.lfmelo.service.CondominiumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CondominiumControllerImpl implements CondominiumController {

    private final CondominiumService condominiumService;

    public CondominiumControllerImpl(CondominiumService condominiumService) {
        this.condominiumService = condominiumService;
    }

    @Override
    public ResponseEntity<List<CondominiumDto>> findAll() {
        return ResponseEntity.ok(condominiumService.findAll());
    }

    @Override
    public ResponseEntity<CondominiumDto> findById(UUID id) {
        return ResponseEntity.ok(condominiumService.findById(id));
    }

    @Override
    public ResponseEntity<CondominiumDto> create(CreateCondominiumRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(condominiumService.create(request));
    }

    @Override
    public ResponseEntity<CondominiumDto> update(UUID id, UpdateCondominiumRequest request) {
        return ResponseEntity.ok(condominiumService.update(id, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID id) {
        condominiumService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
