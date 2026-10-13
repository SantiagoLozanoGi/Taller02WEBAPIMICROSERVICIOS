package edu.unibague.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import edu.unibague.auth.model.Credential;

/**
 * Emite JWT firmados con RS256. El token cumple el contrato del taller:
 * sub = UUID de la credencial, username, email y roles como claims.
 */
@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expirationMinutes;
    private final String issuer;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes,
                      @Value("${app.jwt.issuer:taller02-auth}") String issuer) {
        this.encoder = encoder;
        this.expirationMinutes = expirationMinutes;
        this.issuer = issuer;
    }

    public TokenResult generateToken(Credential credential) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationMinutes, ChronoUnit.MINUTES);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(credential.getId().toString())
                .claim("username", credential.getUsername())
                .claim("email", credential.getEmail())
                .claim("roles", List.of("ROLE_" + credential.getRole().name()))
                .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId("taller02-key")
                .build();

        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResult(token, expiresAt);
    }

    public record TokenResult(String token, Instant expiresAt) {}
}
