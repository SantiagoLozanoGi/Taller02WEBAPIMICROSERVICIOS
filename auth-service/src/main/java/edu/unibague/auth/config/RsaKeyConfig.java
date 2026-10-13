package edu.unibague.auth.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import edu.unibague.auth.security.PemKeyLoader;

/**
 * Carga el par de llaves RSA (PEM) usado para firmar los JWT con RS256.
 * La llave privada solo reside en este servicio, el emisor de tokens.
 */
@Configuration
public class RsaKeyConfig {

    @Bean
    RSAPublicKey rsaPublicKey(@Value("${app.jwt.public-key}") Resource resource) {
        return PemKeyLoader.readPublicKey(resource);
    }

    @Bean
    RSAPrivateKey rsaPrivateKey(@Value("${app.jwt.private-key}") Resource resource) {
        return PemKeyLoader.readPrivateKey(resource);
    }

    @Bean
    JwtEncoder jwtEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("taller02-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();
        JWKSource<SecurityContext> jwkSource = (selector, context) -> selector.select(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
        return NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
    }
}
