package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.condominium.CondominiumDto;
import br.com.lfmelo.dto.condominium.CreateCondominiumRequest;
import br.com.lfmelo.dto.condominium.UpdateCondominiumRequest;
import br.com.lfmelo.entities.Condominium;

public interface CondominiumService {

    List<CondominiumDto> findAll();

    CondominiumDto findById(UUID id);

    CondominiumDto create(CreateCondominiumRequest request);

    CondominiumDto update(UUID id, UpdateCondominiumRequest request);

    void delete(UUID id);

    /**
     * Returns the managed {@link Condominium} entity or throws {@code ResourceNotFoundException}.
     * Reused by services of resources scoped to a condominium (units, residents, staff members).
     */
    Condominium getOrThrow(UUID id);
}
