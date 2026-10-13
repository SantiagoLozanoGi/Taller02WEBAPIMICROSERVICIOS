package edu.unibague.users.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Genera en memoria (y solo en memoria) los pares de llaves RSA que usan las
 * pruebas automatizadas. Ninguna llave se versiona en el repositorio.
 *
 * El par principal representa el "emisor real" (auth-service); el alternativo
 * permite simular tokens firmados por un tercero no confiable.
 */
public final class TestRsaKeys {

    private static final KeyPair MAIN_PAIR = generateKeyPair();
    private static final KeyPair ALTERNATIVE_PAIR = generateKeyPair();

    private TestRsaKeys() {}

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) MAIN_PAIR.getPublic();
    }

    public static RSAPrivateKey privateKey() {
        return (RSAPrivateKey) MAIN_PAIR.getPrivate();
    }

    public static RSAPrivateKey untrustedPrivateKey() {
        return (RSAPrivateKey) ALTERNATIVE_PAIR.getPrivate();
    }

    public static String publicPem() {
        return toPem("PUBLIC KEY", MAIN_PAIR.getPublic().getEncoded());
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("El algoritmo RSA no está disponible en este JVM", ex);
        }
    }

    private static String toPem(String type, byte[] encoded) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
