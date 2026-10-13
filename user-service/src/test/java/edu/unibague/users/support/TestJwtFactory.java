package edu.unibague.users.support;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Fábrica de JWT de prueba firmados con RS256, equivalente a los que emite
 * auth-service. Permite elegir claims y vigencia para probar la validación
 * de firma, expiración e issuer en el Resource Server.
 */
public final class TestJwtFactory {

    public static final String ISSUER = "taller02-auth";

    private TestJwtFactory() {}

    public static String validToken(RSAPrivateKey privateKey, String subject, String username,
                                    String email, List<String> roles) {
        return token(privateKey, ISSUER, subject, username, email, roles, Instant.now().plusSeconds(1800));
    }

    public static String token(RSAPrivateKey privateKey, String issuer, String subject, String username,
                               String email, List<String> roles, Instant expiresAt) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .claim("username", username)
                    .claim("email", email)
                    .claim("roles", roles)
                    .issueTime(new Date())
                    .expirationTime(Date.from(expiresAt))
                    .build();
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("taller02-key").build(), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible firmar el JWT de prueba", ex);
        }
    }
}
