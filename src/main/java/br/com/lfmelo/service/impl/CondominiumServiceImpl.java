package br.com.lfmelo.service.impl;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.condominium.CondominiumDto;
import br.com.lfmelo.dto.condominium.CreateCondominiumRequest;
import br.com.lfmelo.dto.condominium.UpdateCondominiumRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.CondominiumMapper;
import br.com.lfmelo.repository.CondominiumRepository;
import br.com.lfmelo.service.CondominiumService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CondominiumServiceImpl implements CondominiumService {

    private final CondominiumRepository condominiumRepository;

    public CondominiumServiceImpl(CondominiumRepository condominiumRepository) {
        this.condominiumRepository = condominiumRepository;
    }

    @Override
    public List<CondominiumDto> findAll() {
        return condominiumRepository.findAll().stream().map(CondominiumMapper::toDto).toList();
    }

    @Override
    public CondominiumDto findById(UUID id) {
        return CondominiumMapper.toDto(getOrThrow(id));
    }

    @Override
    @Transactional
    public CondominiumDto create(CreateCondominiumRequest request) {
        Condominium condominium = new Condominium();
        condominium.setName(request.name());
        return CondominiumMapper.toDto(condominiumRepository.save(condominium));
    }

    @Override
    @Transactional
    public CondominiumDto update(UUID id, UpdateCondominiumRequest request) {
        Condominium condominium = getOrThrow(id);
        condominium.setName(request.name());
        return CondominiumMapper.toDto(condominiumRepository.save(condominium));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Condominium condominium = getOrThrow(id);
        condominiumRepository.delete(condominium);
    }

    @Override
    public Condominium getOrThrow(UUID id) {
        return condominiumRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + id));
    }
}
