package br.com.lfmelo.controller;

import java.util.List;
import java.util.UUID;
import br.com.lfmelo.dto.CreateStaffMemberRequest;
import br.com.lfmelo.dto.StaffMemberDto;
import br.com.lfmelo.dto.UpdateStaffMemberRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** CRUD contract for the {@code /api/v1/condominiums/{condominiumId}/staff-members} resource. */
@RequestMapping("/api/v1/condominiums/{condominiumId}/staff-members")
public interface StaffMemberController {

    @GetMapping
    ResponseEntity<List<StaffMemberDto>> findAll(@PathVariable UUID condominiumId);

    @GetMapping("/{id}")
    ResponseEntity<StaffMemberDto> findById(@PathVariable UUID condominiumId, @PathVariable UUID id);

    @PostMapping
    ResponseEntity<StaffMemberDto> create(
            @PathVariable UUID condominiumId, @Valid @RequestBody CreateStaffMemberRequest request);

    @PutMapping("/{id}")
    ResponseEntity<StaffMemberDto> update(
            @PathVariable UUID condominiumId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStaffMemberRequest request);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID condominiumId, @PathVariable UUID id);
}
