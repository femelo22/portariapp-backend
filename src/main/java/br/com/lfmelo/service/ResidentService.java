package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.resident.CreateResidentRequest;
import br.com.lfmelo.dto.resident.ResidentDto;
import br.com.lfmelo.dto.resident.UpdateResidentRequest;

public interface ResidentService {

    List<ResidentDto> findAll(UUID condominiumId);

    ResidentDto findById(UUID condominiumId, UUID id);

    ResidentDto create(UUID condominiumId, CreateResidentRequest request);

    ResidentDto update(UUID condominiumId, UUID id, UpdateResidentRequest request);

    void delete(UUID condominiumId, UUID id);
}
