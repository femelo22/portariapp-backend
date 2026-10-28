package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.unit.CreateUnitRequest;
import br.com.lfmelo.dto.unit.UnitDto;
import br.com.lfmelo.dto.unit.UpdateUnitRequest;
import br.com.lfmelo.entities.Unit;

public interface UnitService {

    List<UnitDto> findAll(UUID condominiumId);

    UnitDto findById(UUID condominiumId, UUID id);

    UnitDto create(UUID condominiumId, CreateUnitRequest request);

    UnitDto update(UUID condominiumId, UUID id, UpdateUnitRequest request);

    void delete(UUID condominiumId, UUID id);

    /**
     * Returns the managed {@link Unit} entity, scoped to the given condominium, or throws {@code
     * ResourceNotFoundException}. Reused by services of resources that reference a unit (residents).
     */
    Unit getOrThrow(UUID condominiumId, UUID id);
}
