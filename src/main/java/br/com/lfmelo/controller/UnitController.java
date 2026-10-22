package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.CreateUnitRequest;
import br.com.lfmelo.dto.UnitDto;
import br.com.lfmelo.dto.UpdateUnitRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** CRUD contract for the {@code /api/v1/condominiums/{condominiumId}/units} resource. */
@RequestMapping("/api/v1/condominiums/{condominiumId}/units")
public interface UnitController {

    @GetMapping
    ResponseEntity<List<UnitDto>> findAll(@PathVariable UUID condominiumId);

    @GetMapping("/{id}")
    ResponseEntity<UnitDto> findById(@PathVariable UUID condominiumId, @PathVariable UUID id);

    @PostMapping
    ResponseEntity<UnitDto> create(
            @PathVariable UUID condominiumId, @Valid @RequestBody CreateUnitRequest request);

    @PutMapping("/{id}")
    ResponseEntity<UnitDto> update(
            @PathVariable UUID condominiumId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUnitRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID condominiumId, @PathVariable UUID id);
}
