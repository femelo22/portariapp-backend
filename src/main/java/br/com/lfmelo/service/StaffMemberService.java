package br.com.lfmelo.service;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.staffmember.CreateStaffMemberRequest;
import br.com.lfmelo.dto.staffmember.StaffMemberDto;
import br.com.lfmelo.dto.staffmember.UpdateStaffMemberRequest;

public interface StaffMemberService {

    List<StaffMemberDto> findAll(UUID condominiumId);

    StaffMemberDto findById(UUID condominiumId, UUID id);

    StaffMemberDto create(UUID condominiumId, CreateStaffMemberRequest request);

    StaffMemberDto update(UUID condominiumId, UUID id, UpdateStaffMemberRequest request);

    void delete(UUID condominiumId, UUID id);
}
