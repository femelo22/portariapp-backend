package br.com.lfmelo.service.impl;

import java.time.Instant;
import br.com.lfmelo.dto.auth.LoginRequest;
import br.com.lfmelo.dto.auth.LoginResponse;
import br.com.lfmelo.entities.StaffMember;
import br.com.lfmelo.entities.User;
import br.com.lfmelo.exception.UnauthorizedException;
import br.com.lfmelo.repository.StaffMemberRepository;
import br.com.lfmelo.repository.UserRepository;
import br.com.lfmelo.security.JwtService;
import br.com.lfmelo.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid credentials";

    private final UserRepository userRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            StaffMemberRepository staffMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user =
                userRepository
                        .findByEmailIgnoreCase(request.email())
                        .filter(User::isActive)
                        .filter(u -> u.getPasswordHash() != null)
                        .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                        .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        // Valida se usuario esta ativo E tem um staff member vinculado
        StaffMember staffMember =
                staffMemberRepository
                        .findByUserId(user.getId())
                        .filter(StaffMember::isActive)
                        .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        String token = jwtService.generateToken(staffMember);
        Instant expiresAt = Instant.now().plus(jwtService.getExpiration());
        return new LoginResponse(
                token,
                "Bearer",
                expiresAt,
                staffMember.getId(),
                staffMember.getCondominium().getId(),
                staffMember.getFullName(),
                user.getEmail());
    }
}
