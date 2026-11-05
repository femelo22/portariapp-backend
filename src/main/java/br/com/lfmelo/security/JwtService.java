package br.com.lfmelo.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import br.com.lfmelo.entities.StaffMember;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and validates stateless JWTs for authenticated {@link StaffMember}s. Tokens embed
 * {@code userId}, {@code staffId} and {@code condominiumId} claims so downstream code can
 * enforce multi-tenant access without extra lookups.
 */
@Service
public class JwtService {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_STAFF_ID = "staffId";
    private static final String CLAIM_CONDOMINIUM_ID = "condominiumId";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-minutes:480}") long expirationMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    /** Generates a signed JWT for the given staff member, valid for {@link #expiration}. */
    public String generateToken(StaffMember staffMember) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        return Jwts.builder()
                .subject(staffMember.getUser().getEmail())
                .claim(CLAIM_USER_ID, staffMember.getUser().getId().toString())
                .claim(CLAIM_STAFF_ID, staffMember.getId().toString())
                .claim(CLAIM_CONDOMINIUM_ID, staffMember.getCondominium().getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    /** Returns the configured token lifetime, used to compute {@code expiresAt} for API responses. */
    public Duration getExpiration() {
        return expiration;
    }

    /**
     * Validates the token signature/expiration and extracts the embedded staff claims.
     *
     * @throws JwtException if the token is malformed, expired, or has an invalid signature.
     */
    public AuthenticatedStaff parseToken(String token) {
        Claims claims =
                Jwts.parser().verifyWith(signingKey).build()
                        .parseSignedClaims(token)
                        .getPayload();
        return new AuthenticatedStaff(
                UUID.fromString(claims.get(CLAIM_USER_ID, String.class)),
                UUID.fromString(claims.get(CLAIM_STAFF_ID, String.class)),
                UUID.fromString(claims.get(CLAIM_CONDOMINIUM_ID, String.class)),
                claims.getSubject());
    }
}
