package edu.unibague.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.Resource;

/**
 * Lee llaves RSA en formato PEM (X.509 para la pública, PKCS#8 para la privada)
 * desde cualquier {@link Resource} de Spring (classpath, archivo, etc.).
 */
public final class PemKeyLoader {

    private PemKeyLoader() {}

    public static RSAPublicKey readPublicKey(Resource resource) {
        try {
            String pem = resource.getContentAsString(StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible cargar la llave pública RSA", ex);
        }
    }

    public static RSAPrivateKey readPrivateKey(Resource resource) {
        try {
            String pem = resource.getContentAsString(StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible leer la llave privada RSA", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("No fue posible cargar la llave privada RSA", ex);
        }
    }
}
