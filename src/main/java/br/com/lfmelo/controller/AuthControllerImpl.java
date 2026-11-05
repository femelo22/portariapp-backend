package br.com.lfmelo.controller;

import br.com.lfmelo.dto.auth.LoginRequest;
import br.com.lfmelo.dto.auth.LoginResponse;
import br.com.lfmelo.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthControllerImpl implements AuthController {

    private final AuthService authService;

    public AuthControllerImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
