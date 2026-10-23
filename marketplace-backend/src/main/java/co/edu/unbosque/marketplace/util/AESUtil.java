package co.edu.unbosque.marketplace.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Clase utilitaria para el cifrado y descifrado de datos utilizando el algoritmo AES.
 * Proporciona métodos para encriptar y desencriptar cadenas de texto.
 */
public class AESUtil {

    /** Algoritmo de encriptación utilizado. */
    private final static String ALGORITMO = "AES";

    /** Tipo de cifrado utilizado: AES/CBC/PKCS5Padding. */
    private final static String TIPOCIFRADO = "AES/CBC/PKCS5Padding";

    /** Llave secreta utilizada para la encriptación. */
    private static final String KEY = "llavede16carater";

    /** Vector de inicialización utilizado para la encriptación. */
    private static final String IV = "programacioncomp";

    /**
     * Encripta un texto plano usando la llave y el vector de inicialización proporcionados.
     *
     * @param llave Llave secreta para la encriptación.
     * @param iv Vector de inicialización.
     * @param texto Texto plano a encriptar.
     * @return Texto encriptado en formato Base64.
     */
    public static String encrypt(String llave, String iv, String texto) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(TIPOCIFRADO);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(llave.getBytes(), ALGORITMO);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        byte[] encrypted = null;
        try {
            encrypted = cipher.doFinal(texto.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return new String(encodeBase64(encrypted));
    }
    /** Contructor */
    public AESUtil() {
		// TODO Auto-generated constructor stub
	}
    
    /**
     * Desencripta un texto encriptado usando la llave y el vector de inicialización proporcionados.
     *
     * @param llave Llave secreta para la desencriptación.
     * @param iv Vector de inicialización.
     * @param encrypted Texto encriptado en formato Base64.
     * @return Texto plano desencriptado.
     */
    public static String decrypt(String llave, String iv, String encrypted) {
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance(TIPOCIFRADO);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(llave.getBytes(), ALGORITMO);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
        try {
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        byte[] enc = decodeBase64(encrypted);
        byte[] decrypted = null;
        try {
            decrypted = cipher.doFinal(enc);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            e.printStackTrace();
        }
        return new String(decrypted);
    }

    /**
     * Encripta el contenido de un archivo y guarda el resultado en otro archivo.
     *
     * @param inputPath Ruta del archivo de entrada.
     * @param outputPath Ruta del archivo de salida donde se guardará el contenido encriptado.
     * @throws Exception Si ocurre un error durante la lectura, encriptación o escritura del archivo.
     */
    public static void encryptFile(String inputPath, String outputPath) throws Exception {
        byte[] fileBytes = Files.readAllBytes(Paths.get(inputPath));
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encryptedBytes = cipher.doFinal(fileBytes);
        Files.write(Paths.get(outputPath), encryptedBytes);
        System.out.println("✅ Archivo encriptado: " + outputPath);
    }

    /**
     * Desencripta el contenido de un archivo y guarda el resultado en otro archivo.
     *
     * @param inputPath Ruta del archivo de entrada encriptado.
     * @param outputPath Ruta del archivo de salida donde se guardará el contenido desencriptado.
     * @throws Exception Si ocurre un error durante la lectura, desencriptación o escritura del archivo.
     */
    public static void decryptFile(String inputPath, String outputPath) throws Exception {
        byte[] encryptedBytes = Files.readAllBytes(Paths.get(inputPath));
        Cipher cipher = Cipher.getInstance(ALGORITMO);
        SecretKeySpec keySpec = new SecretKeySpec(KEY.getBytes(), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(IV.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        Files.write(Paths.get(outputPath), decryptedBytes);
        System.out.println("✅ Archivo desencriptado: " + outputPath);
    }

    /**
     * Desencripta un texto encriptado usando la llave y el vector de inicialización por defecto.
     *
     * @param encrypted Texto encriptado en formato Base64.
     * @return Texto plano desencriptado.
     */
    public static String decrypt(String encrypted) {
        String iv = "programacioncomp";
        String key = "llavede16carater";
        return decrypt(key, iv, encrypted);
    }

    /**
     * Encripta un texto plano usando la llave y el vector de inicialización por defecto.
     *
     * @param plainText Texto plano a encriptar.
     * @return Texto encriptado en formato Base64.
     */
    public static String encrypt(String plainText) {
        String iv = "programacioncomp";
        String key = "llavede16carater";
        return encrypt(key, iv, plainText);
    }

    /**
     * Método principal para probar la funcionalidad de encriptación y desencriptación.
     *
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        String text = "asd";

        String iv = "holamundohfhfhtf";
        String key = "holamundohfhfhtf";
        System.out.println(iv.getBytes().length);

        String encoded = encrypt(key, iv, text);
        System.out.println(decrypt(key, iv, encoded));
    }
}
