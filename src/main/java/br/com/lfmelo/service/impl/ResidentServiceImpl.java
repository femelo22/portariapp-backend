package br.com.lfmelo.service.impl;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.resident.CreateResidentRequest;
import br.com.lfmelo.dto.resident.ResidentDto;
import br.com.lfmelo.dto.resident.UpdateResidentRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.Resident;
import br.com.lfmelo.entities.Unit;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.ResidentMapper;
import br.com.lfmelo.repository.ResidentRepository;
import br.com.lfmelo.service.CondominiumService;
import br.com.lfmelo.service.ResidentService;
import br.com.lfmelo.service.UnitService;
import br.com.lfmelo.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ResidentServiceImpl implements ResidentService {

    private final ResidentRepository residentRepository;
    private final CondominiumService condominiumService;
    private final UnitService unitService;
    private final UserService userService;

    public ResidentServiceImpl(
            ResidentRepository residentRepository,
            CondominiumService condominiumService,
            UnitService unitService,
            UserService userService) {
        this.residentRepository = residentRepository;
        this.condominiumService = condominiumService;
        this.unitService = unitService;
        this.userService = userService;
    }

    @Override
    public List<ResidentDto> findAll(UUID condominiumId) {
        condominiumService.getOrThrow(condominiumId);
        return residentRepository.findByCondominiumId(condominiumId).stream()
                .map(ResidentMapper::toDto)
                .toList();
    }

    @Override
    public ResidentDto findById(UUID condominiumId, UUID id) {
        return ResidentMapper.toDto(getOrThrow(condominiumId, id));
    }

    @Override
    @Transactional
    public ResidentDto create(UUID condominiumId, CreateResidentRequest request) {
        if (!condominiumId.equals(request.condominiumId())) {
            throw new BadRequestException("condominiumId in body does not match path");
        }
        Condominium condominium = condominiumService.getOrThrow(condominiumId);
        Unit unit = unitService.getOrThrow(condominiumId, request.unitId());

        Resident resident = new Resident();
        resident.setCondominium(condominium);
        resident.setUnit(unit);
        resident.setUser(resolveUser(request.userId(), null));
        resident.setFullName(request.fullName());
        resident.setPhone(request.phone());
        return ResidentMapper.toDto(residentRepository.save(resident));
    }

    @Override
    @Transactional
    public ResidentDto update(UUID condominiumId, UUID id, UpdateResidentRequest request) {
        Resident resident = getOrThrow(condominiumId, id);
        Unit unit = unitService.getOrThrow(condominiumId, request.unitId());

        resident.setUnit(unit);
        resident.setFullName(request.fullName());
        resident.setPhone(request.phone());
        resident.setActive(request.active());
        return ResidentMapper.toDto(residentRepository.save(resident));
    }

    @Override
    @Transactional
    public void delete(UUID condominiumId, UUID id) {
        Resident resident = getOrThrow(condominiumId, id);
        residentRepository.delete(resident);
    }

    private Resident getOrThrow(UUID condominiumId, UUID id) {
        return residentRepository
                .findByIdAndCondominiumId(id, condominiumId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Resident not found: " + id + " in condominium " + condominiumId));
    }

    private User resolveUser(UUID userId, UUID currentResidentId) {
        if (userId == null) {
            return null;
        }
        User user = userService.getOrThrow(userId);
        boolean alreadyLinked =
                currentResidentId == null
                        ? residentRepository.existsByUserId(userId)
                        : residentRepository.existsByUserIdAndIdNot(userId, currentResidentId);
        if (alreadyLinked) {
            throw new ConflictException("User already linked to another resident: " + userId);
        }
        return user;
    }
}
