package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.CondominiumDto;
import br.com.lfmelo.dto.CreateCondominiumRequest;
import br.com.lfmelo.dto.UpdateCondominiumRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** CRUD contract for the {@code /api/v1/condominiums} resource. */
@RequestMapping("/api/v1/condominiums")
public interface CondominiumController {

    @GetMapping
    ResponseEntity<List<CondominiumDto>> findAll();

    @GetMapping("/{id}")
    ResponseEntity<CondominiumDto> findById(@PathVariable UUID id);

    @PostMapping
    ResponseEntity<CondominiumDto> create(@Valid @RequestBody CreateCondominiumRequest request);

    @PutMapping("/{id}")
    ResponseEntity<CondominiumDto> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateCondominiumRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id);
}
