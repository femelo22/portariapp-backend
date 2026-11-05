package br.com.lfmelo.service;

import br.com.lfmelo.dto.auth.LoginRequest;
import br.com.lfmelo.dto.auth.LoginResponse;

public interface AuthService {

    /**
     * Authenticates a staff member by email+password and issues a JWT.
     *
     * @throws br.com.lfmelo.exception.UnauthorizedException if the credentials are invalid, the
     *     user/staff account is inactive, or the user has no linked staff member.
     */
    LoginResponse login(LoginRequest request);
}
