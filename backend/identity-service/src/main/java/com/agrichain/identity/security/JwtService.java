package com.agrichain.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Issues and validates short-lived JWTs.
 * Each token carries a unique JTI (JWT ID) used as the token-store key.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expiryMinutes;

    public JwtService(
            @Value("${agrichain.jwt.secret}") String secret,
            @Value("${agrichain.jwt.expiry-minutes:30}") long expiryMinutes) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMinutes = expiryMinutes;
    }

    /**
     * Issues a signed JWT for the given subject (username).
     *
     * @param subject  the username
     * @param tokenId  a pre-generated UUID used as the JTI claim
     * @return signed compact JWT string
     */
    public String issue(String subject, UUID tokenId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expiryMinutes * 60);

        return Jwts.builder()
                .id(tokenId.toString())
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Parses and validates a JWT, returning its claims.
     *
     * @throws JwtException if the token is malformed, expired, or has an invalid signature
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the JTI (token ID) from a raw JWT string without full validation.
     * Used only when the token has already been validated.
     */
    public UUID extractTokenId(String token) {
        Claims claims = parse(token);
        return UUID.fromString(claims.getId());
    }

    public long getExpiryMinutes() { return expiryMinutes; }
}
