package br.com.lfmelo.security;

import java.util.Map;
import java.util.UUID;
import br.com.lfmelo.controller.CondominiumController;
import br.com.lfmelo.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Enforces multi-tenant isolation: an authenticated {@link AuthenticatedStaff} may only operate
 * on resources that belong to their own condominium, as embedded in the JWT.
 *
 * <ul>
 *   <li>Nested resources ({@code /api/v1/condominiums/{condominiumId}/...}) must match the
 *       staff's condominium, otherwise {@link ForbiddenException} (403) is thrown.
 *   <li>{@link CondominiumController} itself has no {@code admin} role yet, so staff can only
 *       read/update their own condominium ({@code GET}/{@code PUT /{id}}); listing, creating and
 *       deleting condominiums is blocked entirely.
 * </ul>
 */
@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AuthenticatedStaff staff = SecurityUtils.currentStaff().orElse(null);
        if (staff == null) {
            return true;
        }

        Map<String, String> pathVariables = pathVariables(request);

        String nestedCondominiumId = pathVariables.get("condominiumId");
        if (nestedCondominiumId != null) {
            requireSameCondominium(staff, nestedCondominiumId);
            return true;
        }

        if (CondominiumController.class.isAssignableFrom(handlerMethod.getBeanType())) {
            enforceCondominiumRootAccess(staff, request, pathVariables);
        }
        return true;
    }

    private void enforceCondominiumRootAccess(
            AuthenticatedStaff staff, HttpServletRequest request, Map<String, String> pathVariables) {
        String id = pathVariables.get("id");
        if (id == null) {
            throw new ForbiddenException(
                    "Staff members cannot " + request.getMethod() + " the condominiums collection");
        }
        if (HttpMethod.DELETE.matches(request.getMethod())) {
            throw new ForbiddenException("Staff members cannot delete condominiums");
        }
        requireSameCondominium(staff, id);
    }

    private void requireSameCondominium(AuthenticatedStaff staff, String condominiumIdValue) {
        UUID condominiumId;
        try {
            condominiumId = UUID.fromString(condominiumIdValue);
        } catch (IllegalArgumentException ex) {
            return;
        }
        if (!staff.condominiumId().equals(condominiumId)) {
            throw new ForbiddenException("Access denied to condominium: " + condominiumId);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> pathVariables(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        return attribute instanceof Map<?, ?> map ? (Map<String, String>) map : Map.of();
    }
}
