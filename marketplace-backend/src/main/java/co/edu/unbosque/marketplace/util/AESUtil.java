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
 * Cifrado de datos sensibles con AES-GCM.
 * <p>
 * La clave sale de la variable de entorno {@code AES_SECRET_KEY} (Base64 de 16,
 * 24 o 32 bytes) y nunca se escribe en el código ni en las propiedades. Con
 * esta clase cifro el correo de los usuarios y el secreto del segundo factor,
 * calculo el hash con el que busco los correos y derivo la clave que firma los
 * JWT.
 * </p>
 * <p>
 * Formato de lo cifrado: los 12 bytes del IV (aleatorio en cada cifrado)
 * seguidos del texto cifrado con su etiqueta de autenticación de 16 bytes. Los
 * textos se entregan en Base64. Como el IV cambia cada vez, cifrar dos veces el
 * mismo texto da resultados distintos; por eso, para buscar, uso
 * {@link #hash(String)}.
 * </p>
 * <p>
 * Importante: si cambia la clave, lo que se cifró antes deja de poder leerse.
 * Todo el equipo debe usar la misma clave cuando comparte la base de datos.
 * </p>
 */
public final class AESUtil {

	/** Algoritmo de la clave. */
	private static final String ALGORITMO = "AES";

	/** Transformación completa: AES en modo GCM, que no usa relleno. */
	private static final String TIPO_CIFRADO = "AES/GCM/NoPadding";

	/** Algoritmo del hash con clave y de la derivación de claves. */
	private static final String ALGORITMO_HASH = "HmacSHA256";

	/** Largo del IV en bytes (el recomendado para GCM). */
	private static final int TAMANO_IV = 12;

	/** Largo de la etiqueta de autenticación en bits. */
	private static final int TAMANO_TAG = 128;

	/** Largo mínimo de un dato cifrado: IV más la etiqueta de 16 bytes. */
	private static final int TAMANO_MINIMO = TAMANO_IV + 16;

	/** Generador de los IV aleatorios. */
	private static final SecureRandom RANDOM = new SecureRandom();

	/** Clase utilitaria: no se instancia. */
	private AESUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/**
	 * Lee y valida la clave de la variable de entorno {@code AES_SECRET_KEY}.
	 *
	 * @return clave AES lista para usar
	 * @throws IllegalStateException si la variable no existe, no está en Base64 o
	 *                               no mide 16, 24 o 32 bytes
	 */
	private static SecretKeySpec obtenerClave() {
		String valor = System.getenv("AES_SECRET_KEY");

		if (valor == null || valor.isBlank()) {
			throw new IllegalStateException("No se ha configurado AES_SECRET_KEY");
		}

		byte[] clave;

		try {
			clave = Base64.getDecoder().decode(valor);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("AES_SECRET_KEY debe estar codificada en Base64", e);
		}

		if (clave.length != 16 && clave.length != 24 && clave.length != 32) {
			throw new IllegalStateException("La clave AES debe tener 16, 24 o 32 bytes");
		}

		return new SecretKeySpec(clave, ALGORITMO);
	}

	/**
	 * Genera un IV aleatorio nuevo. Nunca se reutiliza un IV con la misma clave.
	 *
	 * @return IV de {@link #TAMANO_IV} bytes
	 */
	private static byte[] generarIV() {
		byte[] iv = new byte[TAMANO_IV];
		RANDOM.nextBytes(iv);
		return iv;
	}

	/**
	 * Prepara un {@link Cipher} de AES-GCM con la clave del sistema.
	 *
	 * @param modo {@link Cipher#ENCRYPT_MODE} o {@link Cipher#DECRYPT_MODE}
	 * @param iv   IV que se usa en la operación
	 * @return cifrador inicializado
	 * @throws GeneralSecurityException si el proveedor criptográfico falla
	 */
	private static Cipher crearCipher(int modo, byte[] iv) throws GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(TIPO_CIFRADO);
		GCMParameterSpec parametros = new GCMParameterSpec(TAMANO_TAG, iv);
		cipher.init(modo, obtenerClave(), parametros);
		return cipher;
	}

	/**
	 * Cifra bytes y les antepone el IV usado.
	 *
	 * @param datos bytes en claro
	 * @return IV seguido del texto cifrado
	 * @throws GeneralSecurityException si el cifrado falla
	 */
	private static byte[] cifrarBytes(byte[] datos) throws GeneralSecurityException {

		byte[] iv = generarIV();
		Cipher cipher = crearCipher(Cipher.ENCRYPT_MODE, iv);
		byte[] cifrado = cipher.doFinal(datos);

		return ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
	}

	/**
	 * Separa el IV del texto cifrado y lo descifra. GCM comprueba la etiqueta de
	 * autenticación, así que si el dato fue alterado o se cifró con otra clave la
	 * operación falla.
	 *
	 * @param datos IV seguido del texto cifrado
	 * @return bytes en claro
	 * @throws GeneralSecurityException si la etiqueta no coincide o el descifrado
	 *                                  falla
	 * @throws IllegalArgumentException si los datos son demasiado cortos
	 */
	private static byte[] descifrarBytes(byte[] datos) throws GeneralSecurityException {

		if (datos.length < TAMANO_MINIMO) {
			throw new IllegalArgumentException("Los datos cifrados no tienen un formato válido");
		}

		ByteBuffer buffer = ByteBuffer.wrap(datos);
		byte[] iv = new byte[TAMANO_IV];
		buffer.get(iv);

		byte[] cifrado = new byte[buffer.remaining()];
		buffer.get(cifrado);

		Cipher cipher = crearCipher(Cipher.DECRYPT_MODE, iv);
		return cipher.doFinal(cifrado);
	}

	/**
	 * Cifra un texto.
	 *
	 * @param texto texto en claro
	 * @return texto cifrado en Base64 (distinto cada vez, por el IV aleatorio)
	 * @throws NullPointerException  si el texto es {@code null}
	 * @throws IllegalStateException si falta la clave o el cifrado falla
	 */
	public static String encrypt(String texto) {
		Objects.requireNonNull(texto, "El texto no puede ser null");

		try {
			byte[] datos = texto.getBytes(StandardCharsets.UTF_8);
			return Base64.getEncoder().encodeToString(cifrarBytes(datos));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Error al cifrar el texto", e);
		}
	}

	/**
	 * Descifra un texto que se cifró con {@link #encrypt(String)}.
	 *
	 * @param encrypted texto cifrado en Base64
	 * @return texto en claro
	 * @throws NullPointerException     si el texto es {@code null}
	 * @throws IllegalArgumentException si no es Base64 válido, fue alterado o se
	 *                                  cifró con otra clave
	 */
	public static String decrypt(String encrypted) {
		Objects.requireNonNull(encrypted, "El texto cifrado no puede ser null");

		try {
			byte[] datos = Base64.getDecoder().decode(encrypted);
			byte[] descifrados = descifrarBytes(datos);

			return new String(descifrados, StandardCharsets.UTF_8);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			throw new IllegalArgumentException("No fue posible descifrar el texto", e);
		}
	}

	/**
	 * Calcula un HMAC-SHA256 del texto con la clave del sistema. A diferencia de
	 * {@link #encrypt(String)}, el mismo texto siempre da el mismo resultado, y
	 * por eso sirve para buscar en la base de datos (por ejemplo, el correo) sin
	 * guardar el valor en claro.
	 *
	 * @param texto texto a resumir
	 * @return hash en Base64
	 * @throws NullPointerException  si el texto es {@code null}
	 * @throws IllegalStateException si falta la clave o el cálculo falla
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

	/**
	 * Deriva una clave secundaria de 32 bytes a partir de {@code AES_SECRET_KEY},
	 * para no tener que configurar otra variable de entorno. El propósito separa
	 * las claves: con propósitos distintos salen claves distintas, y ninguna
	 * permite recuperar la clave AES original.
	 *
	 * @param proposito para qué se usará la clave, por ejemplo {@code "jwt"}
	 * @return clave derivada de 32 bytes
	 * @throws NullPointerException  si el propósito es {@code null}
	 * @throws IllegalStateException si falta la clave o el cálculo falla
	 */
	public static byte[] derivarClave(String proposito) {
		Objects.requireNonNull(proposito, "El propósito no puede ser null");

		try {
			Mac mac = Mac.getInstance(ALGORITMO_HASH);
			mac.init(new SecretKeySpec(obtenerClave().getEncoded(), ALGORITMO_HASH));
			return mac.doFinal(("clave-derivada:" + proposito).getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("Error al derivar la clave", e);
		}
	}

	/**
	 * Cifra un archivo completo y escribe el resultado en otro.
	 *
	 * @param inputPath  ruta del archivo en claro
	 * @param outputPath ruta donde se escribe el archivo cifrado
	 * @throws IOException              si no se puede leer o escribir
	 * @throws GeneralSecurityException si el cifrado falla
	 */
	public static void encryptFile(String inputPath, String outputPath) throws IOException, GeneralSecurityException {

		Path entrada = Paths.get(inputPath);
		Path salida = Paths.get(outputPath);
		byte[] datos = Files.readAllBytes(entrada);

		Files.write(salida, cifrarBytes(datos));
	}

	/**
	 * Descifra un archivo que se cifró con
	 * {@link #encryptFile(String, String)}.
	 *
	 * @param inputPath  ruta del archivo cifrado
	 * @param outputPath ruta donde se escribe el archivo en claro
	 * @throws IOException              si no se puede leer o escribir
	 * @throws GeneralSecurityException si la clave no coincide o el archivo fue
	 *                                  alterado
	 */
	public static void decryptFile(String inputPath, String outputPath) throws IOException, GeneralSecurityException {

		Path entrada = Paths.get(inputPath);
		Path salida = Paths.get(outputPath);
		byte[] datos = Files.readAllBytes(entrada);

		Files.write(salida, descifrarBytes(datos));
	}
}
