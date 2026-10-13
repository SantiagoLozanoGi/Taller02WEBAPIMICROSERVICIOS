package edu.unibague.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import edu.unibague.auth.model.Credential;
import edu.unibague.auth.model.Role;
import edu.unibague.auth.support.TestRsaKeys;

/**
 * Prueba el contrato completo del JWT: firma RS256 verificable con la llave
 * pública y claims sub (UUID), username, email y roles según el taller.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        RSAKey rsaKey = new RSAKey.Builder(TestRsaKeys.publicKey())
                .privateKey(TestRsaKeys.privateKey())
                .keyID("taller02-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();
        JWKSource<SecurityContext> jwkSource = (selector, context) -> selector.select(new JWKSet(rsaKey));
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(jwkSource);

        jwtService = new JwtService(encoder, 60, "taller02-auth");
        decoder = NimbusJwtDecoder.withPublicKey(TestRsaKeys.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }

    @Test
    void generatesTokenWithContractClaimsForUser() {
        Credential credential = credential(Role.USER, "student", "student@taller.local");

        JwtService.TokenResult result = jwtService.generateToken(credential);

        assertThat(result.token()).isNotBlank();
        assertThat(result.expiresAt()).isAfter(Instant.now());

        Jwt jwt = decoder.decode(result.token());
        assertThat(jwt.getSubject()).isEqualTo(credential.getId().toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("student");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("student@taller.local");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_USER");
        // El issuer se verifica como claim crudo: en Spring Security 7
        // Jwt.getIssuer() exige un valor URL, pero el validador real
        // (JwtClaimValidator) compara el claim 'iss' directamente.
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("taller02-auth");
        // Los timestamps de los claims JWT se serializan con precision de segundos.
        assertThat(jwt.getExpiresAt()).isEqualTo(result.expiresAt().truncatedTo(ChronoUnit.SECONDS));
    }

    @Test
    void generatesTokenWithAdminRoleClaim() {
        Credential credential = credential(Role.ADMIN, "admin", "admin@taller.local");

        Jwt jwt = decoder.decode(jwtService.generateToken(credential).token());

        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_ADMIN");
    }

    @Test
    void tokenDoesNotContainThePasswordHash() {
        Credential credential = credential(Role.USER, "student", "student@taller.local");

        Jwt jwt = decoder.decode(jwtService.generateToken(credential).token());

        assertThat(jwt.getClaims()).doesNotContainKey("password");
        assertThat(jwt.getClaims()).doesNotContainKey("passwordHash");
    }

    private Credential credential(Role role, String username, String email) {
        Credential credential = new Credential();
        credential.setId(UUID.randomUUID());
        credential.setUsername(username);
        credential.setEmail(email);
        credential.setRole(role);
        return credential;
    }
}
