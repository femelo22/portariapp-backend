package br.com.lfmelo.service.impl;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.unit.CreateUnitRequest;
import br.com.lfmelo.dto.unit.UnitDto;
import br.com.lfmelo.dto.unit.UpdateUnitRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Unit;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.UnitMapper;
import br.com.lfmelo.repository.UnitRepository;
import br.com.lfmelo.service.CondominiumService;
import br.com.lfmelo.service.UnitService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;
    private final CondominiumService condominiumService;

    public UnitServiceImpl(UnitRepository unitRepository, CondominiumService condominiumService) {
        this.unitRepository = unitRepository;
        this.condominiumService = condominiumService;
    }

    @Override
    public List<UnitDto> findAll(UUID condominiumId) {
        condominiumService.getOrThrow(condominiumId);
        return unitRepository.findByCondominiumId(condominiumId).stream().map(UnitMapper::toDto).toList();
    }

    @Override
    public UnitDto findById(UUID condominiumId, UUID id) {
        return UnitMapper.toDto(getOrThrow(condominiumId, id));
    }

    @Override
    @Transactional
    public UnitDto create(UUID condominiumId, CreateUnitRequest request) {
        if (!condominiumId.equals(request.condominiumId())) {
            throw new BadRequestException("condominiumId in body does not match path");
        }
        Condominium condominium = condominiumService.getOrThrow(condominiumId);
        if (unitRepository.existsByCondominiumIdAndIdentifierIgnoreCase(condominiumId, request.identifier())) {
            throw new ConflictException("Unit identifier already in use: " + request.identifier());
        }
        Unit unit = new Unit();
        unit.setCondominium(condominium);
        unit.setIdentifier(request.identifier());
        return UnitMapper.toDto(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public UnitDto update(UUID condominiumId, UUID id, UpdateUnitRequest request) {
        Unit unit = getOrThrow(condominiumId, id);
        if (unitRepository.existsByCondominiumIdAndIdentifierIgnoreCaseAndIdNot(
                condominiumId, request.identifier(), id)) {
            throw new ConflictException("Unit identifier already in use: " + request.identifier());
        }
        unit.setIdentifier(request.identifier());
        return UnitMapper.toDto(unitRepository.save(unit));
    }

    @Override
    @Transactional
    public void delete(UUID condominiumId, UUID id) {
        Unit unit = getOrThrow(condominiumId, id);
        unitRepository.delete(unit);
    }

    @Override
    public Unit getOrThrow(UUID condominiumId, UUID id) {
        return unitRepository
                .findByIdAndCondominiumId(id, condominiumId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Unit not found: " + id + " in condominium " + condominiumId));
    }
}
