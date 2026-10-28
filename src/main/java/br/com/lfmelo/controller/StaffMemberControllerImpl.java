package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.staffmember.CreateStaffMemberRequest;
import br.com.lfmelo.dto.staffmember.StaffMemberDto;
import br.com.lfmelo.dto.staffmember.UpdateStaffMemberRequest;
import br.com.lfmelo.service.StaffMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffMemberControllerImpl implements StaffMemberController {

    private final StaffMemberService staffMemberService;

    public StaffMemberControllerImpl(StaffMemberService staffMemberService) {
        this.staffMemberService = staffMemberService;
    }

    @Override
    public ResponseEntity<List<StaffMemberDto>> findAll(UUID condominiumId) {
        return ResponseEntity.ok(staffMemberService.findAll(condominiumId));
    }

    @Override
    public ResponseEntity<StaffMemberDto> findById(UUID condominiumId, UUID id) {
        return ResponseEntity.ok(staffMemberService.findById(condominiumId, id));
    }

    @Override
    public ResponseEntity<StaffMemberDto> create(UUID condominiumId, CreateStaffMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(staffMemberService.create(condominiumId, request));
    }

    @Override
    public ResponseEntity<StaffMemberDto> update(
            UUID condominiumId, UUID id, UpdateStaffMemberRequest request) {
        return ResponseEntity.ok(staffMemberService.update(condominiumId, id, request));
    }

    @Override
    public ResponseEntity<Void> delete(UUID condominiumId, UUID id) {
        staffMemberService.delete(condominiumId, id);
        return ResponseEntity.noContent().build();
    }
}
