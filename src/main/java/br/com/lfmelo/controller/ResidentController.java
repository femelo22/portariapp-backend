package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.CreateResidentRequest;
import br.com.lfmelo.dto.ResidentDto;
import br.com.lfmelo.dto.UpdateResidentRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** CRUD contract for the {@code /api/v1/condominiums/{condominiumId}/residents} resource. */
@RequestMapping("/api/v1/condominiums/{condominiumId}/residents")
public interface ResidentController {

    @GetMapping
    ResponseEntity<List<ResidentDto>> findAll(@PathVariable UUID condominiumId);

    @GetMapping("/{id}")
    ResponseEntity<ResidentDto> findById(@PathVariable UUID condominiumId, @PathVariable UUID id);

    @PostMapping
    ResponseEntity<ResidentDto> create(
            @PathVariable UUID condominiumId, @Valid @RequestBody CreateResidentRequest request);

    @PutMapping("/{id}")
    ResponseEntity<ResidentDto> update(
            @PathVariable UUID condominiumId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateResidentRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID condominiumId, @PathVariable UUID id);
}
