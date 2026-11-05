package br.com.lfmelo.controller;

import br.com.lfmelo.dto.auth.LoginRequest;
import br.com.lfmelo.dto.auth.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/** Authentication contract for the {@code /api/v1/auth} resource. */
@RequestMapping("/api/v1/auth")
public interface AuthController {

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
