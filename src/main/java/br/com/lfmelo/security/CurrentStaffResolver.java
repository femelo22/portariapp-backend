package br.com.lfmelo.security;

import java.util.UUID;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.exception.ForbiddenException;
import br.com.lfmelo.exception.UnauthorizedException;
import br.com.lfmelo.repository.StaffMemberRepository;
import org.springframework.stereotype.Component;

@Component
public class CurrentStaffResolver {

    private final StaffMemberRepository staffMemberRepository;

    public CurrentStaffResolver(StaffMemberRepository staffMemberRepository) {
        this.staffMemberRepository = staffMemberRepository;
    }

    public StaffMember requireActive(UUID condominiumId) {

        AuthenticatedStaff principal = SecurityUtils.currentStaff()
                .orElseThrow(() -> new UnauthorizedException("Authentication required"));

        if (!condominiumId.equals(principal.condominiumId())) {
            throw new ForbiddenException("Access denied to condominium: " + condominiumId);
        }

        return staffMemberRepository.findByIdAndCondominiumId(principal.staffId(), condominiumId)
                .filter(StaffMember::isActive)
                .filter(staff -> staff.getUser() != null
                        && staff.getUser().isActive()
                        && principal.userId().equals(staff.getUser().getId()))
                .orElseThrow(() -> new ForbiddenException("Authenticated staff member is not active"));
    }
}
