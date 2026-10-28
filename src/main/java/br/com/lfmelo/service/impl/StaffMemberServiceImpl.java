package br.com.lfmelo.service.impl;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.staffmember.CreateStaffMemberRequest;
import br.com.lfmelo.dto.staffmember.StaffMemberDto;
import br.com.lfmelo.dto.staffmember.UpdateStaffMemberRequest;
import br.com.lfmelo.entities.Condominium;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.exception.BadRequestException;
import br.com.lfmelo.exception.ConflictException;
import br.com.lfmelo.exception.ResourceNotFoundException;
import br.com.lfmelo.mapper.StaffMemberMapper;
import br.com.lfmelo.repository.StaffMemberRepository;
import br.com.lfmelo.service.CondominiumService;
import br.com.lfmelo.service.StaffMemberService;
import br.com.lfmelo.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StaffMemberServiceImpl implements StaffMemberService {

    private final StaffMemberRepository staffMemberRepository;
    private final CondominiumService condominiumService;
    private final UserService userService;

    public StaffMemberServiceImpl(
            StaffMemberRepository staffMemberRepository,
            CondominiumService condominiumService,
            UserService userService) {
        this.staffMemberRepository = staffMemberRepository;
        this.condominiumService = condominiumService;
        this.userService = userService;
    }

    @Override
    public List<StaffMemberDto> findAll(UUID condominiumId) {
        condominiumService.getOrThrow(condominiumId);
        return staffMemberRepository.findByCondominiumId(condominiumId).stream()
                .map(StaffMemberMapper::toDto)
                .toList();
    }

    @Override
    public StaffMemberDto findById(UUID condominiumId, UUID id) {
        return StaffMemberMapper.toDto(getOrThrow(condominiumId, id));
    }

    @Override
    @Transactional
    public StaffMemberDto create(UUID condominiumId, CreateStaffMemberRequest request) {
        if (!condominiumId.equals(request.condominiumId())) {
            throw new BadRequestException("condominiumId in body does not match path");
        }
        Condominium condominium = condominiumService.getOrThrow(condominiumId);

        StaffMember staffMember = new StaffMember();
        staffMember.setCondominium(condominium);
        staffMember.setUser(resolveUser(request.userId(), null));
        staffMember.setFullName(request.fullName());
        return StaffMemberMapper.toDto(staffMemberRepository.save(staffMember));
    }

    @Override
    @Transactional
    public StaffMemberDto update(UUID condominiumId, UUID id, UpdateStaffMemberRequest request) {
        StaffMember staffMember = getOrThrow(condominiumId, id);
        staffMember.setFullName(request.fullName());
        staffMember.setActive(request.active());
        return StaffMemberMapper.toDto(staffMemberRepository.save(staffMember));
    }

    @Override
    @Transactional
    public void delete(UUID condominiumId, UUID id) {
        StaffMember staffMember = getOrThrow(condominiumId, id);
        staffMemberRepository.delete(staffMember);
    }

    private StaffMember getOrThrow(UUID condominiumId, UUID id) {
        return staffMemberRepository
                .findByIdAndCondominiumId(id, condominiumId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Staff member not found: " + id + " in condominium " + condominiumId));
    }

    private User resolveUser(UUID userId, UUID currentStaffMemberId) {
        if (userId == null) {
            return null;
        }
        User user = userService.getOrThrow(userId);
        boolean alreadyLinked =
                currentStaffMemberId == null
                        ? staffMemberRepository.existsByUserId(userId)
                        : staffMemberRepository.existsByUserIdAndIdNot(userId, currentStaffMemberId);
        if (alreadyLinked) {
            throw new ConflictException("User already linked to another staff member: " + userId);
        }
        return user;
    }
}
