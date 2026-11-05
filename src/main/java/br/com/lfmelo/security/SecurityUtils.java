package br.com.lfmelo.security;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Convenience accessor for the {@link AuthenticatedStaff} principal set by {@link JwtAuthenticationFilter}. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Returns the currently authenticated staff member, if the request carries a valid JWT. */
    public static Optional<AuthenticatedStaff> currentStaff() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedStaff staff) {
            return Optional.of(staff);
        }
        return Optional.empty();
    }
}
