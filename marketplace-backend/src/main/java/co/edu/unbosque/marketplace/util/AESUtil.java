
package co.edu.unbosque.marketplace.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilidad para cifrar y descifrar textos y archivos mediante AES-GCM.
 */
public final class AESUtil {

    private static final String ALGORITMO = "AES";
    private static final String TIPO_CIFRADO = "AES/GCM/NoPadding";
    private static final String ALGORITMO_HASH = "HmacSHA256";
    private static final int TAMANO_IV = 12;
    private static final int TAMANO_TAG = 128;
    private static final int TAMANO_MINIMO = TAMANO_IV + 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private AESUtil() {
        throw new IllegalStateException("Clase utilitaria");
    }

    private static SecretKeySpec obtenerClave() {
        String valor = System.getenv("AES_SECRET_KEY");

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "No se ha configurado AES_SECRET_KEY");
        }

        byte[] clave;

        try {
            clave = Base64.getDecoder().decode(valor);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "AES_SECRET_KEY debe estar codificada en Base64", e);
        }

        if (clave.length != 16 && clave.length != 24
                && clave.length != 32) {
            throw new IllegalStateException(
                    "La clave AES debe tener 16, 24 o 32 bytes");
        }

        return new SecretKeySpec(clave, ALGORITMO);
    }

    private static byte[] generarIV() {
        byte[] iv = new byte[TAMANO_IV];
        RANDOM.nextBytes(iv);
        return iv;
    }

    private static Cipher crearCipher(int modo, byte[] iv)
            throws GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(TIPO_CIFRADO);
        GCMParameterSpec parametros = new GCMParameterSpec(TAMANO_TAG, iv);
        cipher.init(modo, obtenerClave(), parametros);
        return cipher;
    }

    private static byte[] cifrarBytes(byte[] datos)
            throws GeneralSecurityException {

        byte[] iv = generarIV();
        Cipher cipher = crearCipher(Cipher.ENCRYPT_MODE, iv);
        byte[] cifrado = cipher.doFinal(datos);

        return ByteBuffer.allocate(iv.length + cifrado.length)
                .put(iv)
                .put(cifrado)
                .array();
    }

    private static byte[] descifrarBytes(byte[] datos)
            throws GeneralSecurityException {

        if (datos.length < TAMANO_MINIMO) {
            throw new IllegalArgumentException(
                    "Los datos cifrados no tienen un formato válido");
        }

        ByteBuffer buffer = ByteBuffer.wrap(datos);
        byte[] iv = new byte[TAMANO_IV];
        buffer.get(iv);

        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);

        Cipher cipher = crearCipher(Cipher.DECRYPT_MODE, iv);
        return cipher.doFinal(cifrado);
    }

    public static String encrypt(String texto) {
        Objects.requireNonNull(texto, "El texto no puede ser null");

        try {
            byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
            return Base64.getEncoder()
                    .encodeToString(cifrarBytes(datos));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al cifrar el texto", e);
        }
    }

    public static String decrypt(String encrypted) {
        Objects.requireNonNull(
                encrypted, "El texto cifrado no puede ser null");

        try {
            byte[] datos = Base64.getDecoder().decode(encrypted);
            byte[] descifrados = descifrarBytes(datos);

            return new String(descifrados, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "No fue posible descifrar el texto", e);
        }
    }

    /**
     * Calcula un HMAC-SHA256 determinista del texto. A diferencia de
     * {@link #encrypt(String)}, el mismo texto siempre produce el mismo
     * resultado, por eso sirve para buscar en la base de datos (por ejemplo,
     * el correo) sin guardar el valor en claro.
     */
    public static String hash(String texto) {
        Objects.requireNonNull(texto, "El texto no puede ser null");

        try {
            Mac mac = Mac.getInstance(ALGORITMO_HASH);
            mac.init(new SecretKeySpec(obtenerClave().getEncoded(), ALGORITMO_HASH));
            byte[] resultado = mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Error al calcular el hash", e);
        }
    }

    public static void encryptFile(String inputPath, String outputPath)
            throws IOException, GeneralSecurityException {

        Path entrada = Paths.get(inputPath);
        Path salida = Paths.get(outputPath);
        byte[] datos = Files.readAllBytes(entrada);

        Files.write(salida, cifrarBytes(datos));
    }

    public static void decryptFile(String inputPath, String outputPath)
            throws IOException, GeneralSecurityException {

        Path entrada = Paths.get(inputPath);
        Path salida = Paths.get(outputPath);
        byte[] datos = Files.readAllBytes(entrada);

        Files.write(salida, descifrarBytes(datos));
    }
}
