package edu.unibague.auth.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Genera en memoria (y solo en memoria) un par de llaves RSA para las
 * pruebas automatizadas. Ninguna llave, ni pública ni privada, se versiona
 * en el repositorio: cada ejecución de la suite genera su propio par.
 *
 * La pública se expone en formato X.509 PEM y la privada en PKCS#8 PEM,
 * exactamente los formatos que espera PemKeyLoader.
 */
public final class TestRsaKeys {

    private static final KeyPair KEY_PAIR = generateKeyPair();

    private TestRsaKeys() {}

    public static RSAPublicKey publicKey() {
        return (RSAPublicKey) KEY_PAIR.getPublic();
    }

    public static RSAPrivateKey privateKey() {
        return (RSAPrivateKey) KEY_PAIR.getPrivate();
    }

    public static String publicPem() {
        return toPem("PUBLIC KEY", KEY_PAIR.getPublic().getEncoded());
    }

    public static String privatePem() {
        return toPem("PRIVATE KEY", KEY_PAIR.getPrivate().getEncoded());
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
