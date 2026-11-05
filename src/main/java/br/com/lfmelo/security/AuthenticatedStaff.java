package br.com.lfmelo.security;

import java.util.UUID;

/**
 * Principal stored in the {@link org.springframework.security.core.context.SecurityContext}
 * for an authenticated {@code StaffMember}, built from the validated JWT claims.
 */
public record AuthenticatedStaff(UUID userId, UUID staffId, UUID condominiumId, String email) {
}
