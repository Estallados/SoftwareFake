package co.edu.unbosque.marketplace.util;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;

/**
 * Códigos de un solo uso basados en tiempo (TOTP, RFC 6238), compatibles con
 * Google Authenticator y similares: HMAC-SHA1, 6 dígitos, pasos de 30 segundos.
 *
 * @see "RNF-033"
 */
public final class TotpUtil {

	private static final int PASO_SEGUNDOS = 30;
	private static final int DIGITOS = 6;
	private static final int MODULO = 1_000_000;
	private static final int BYTES_SECRETO = 20;
	/** Se aceptan el paso anterior y el siguiente por diferencias de reloj. */
	private static final int TOLERANCIA_PASOS = 1;
	private static final String ALGORITMO = "HmacSHA1";
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final Base32 BASE32 = new Base32();

	private TotpUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/** Genera un secreto nuevo en Base32 (sin relleno). */
	public static String generarSecreto() {
		byte[] bytes = new byte[BYTES_SECRETO];
		RANDOM.nextBytes(bytes);
		return BASE32.encodeToString(bytes).replace("=", "");
	}

	/**
	 * URI otpauth para registrar el secreto en una app autenticadora (código QR).
	 */
	public static String uri(String emisor, String cuenta, String secreto) {
		return "otpauth://totp/" + emisor + ":" + cuenta + "?secret=" + secreto + "&issuer=" + emisor + "&digits="
				+ DIGITOS + "&period=" + PASO_SEGUNDOS;
	}

	/** Código de 6 dígitos para un instante. */
	public static String codigo(String secreto, Instant instante) {
		return codigoPara(secreto, instante.getEpochSecond() / PASO_SEGUNDOS);
	}

	/** Verifica un código con la tolerancia de ±1 paso. */
	public static boolean verificar(String secreto, String codigo) {
		if (secreto == null || codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
			return false;
		}
		long pasoActual = Instant.now().getEpochSecond() / PASO_SEGUNDOS;
		byte[] recibido = codigo.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
		for (int i = -TOLERANCIA_PASOS; i <= TOLERANCIA_PASOS; i++) {
			byte[] esperado = codigoPara(secreto, pasoActual + i)
					.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
			if (MessageDigest.isEqual(esperado, recibido)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calcula el código de un paso (RFC 6238 / RFC 4226).
	 * <p>
	 * java:S4790 revisado: HMAC-SHA1 es el algoritmo del estándar TOTP y el que
	 * usan las apps autenticadoras; cambiarlo rompe la compatibilidad y los
	 * segundos factores ya registrados. La debilidad de SHA-1 son las colisiones,
	 * que no afectan a HMAC: aquí no se guarda ni se compara ningún hash, solo se
	 * deriva un código de 6 dígitos que vence a los 30 segundos.
	 * </p>
	 */
	@SuppressWarnings("java:S4790")
	private static String codigoPara(String secreto, long paso) {
		try {
			byte[] clave = BASE32.decode(secreto);
			Mac mac = Mac.getInstance(ALGORITMO);
			mac.init(new SecretKeySpec(clave, ALGORITMO));
			byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
			int desplazamiento = hash[hash.length - 1] & 0x0f;
			int binario = ((hash[desplazamiento] & 0x7f) << 24) | ((hash[desplazamiento + 1] & 0xff) << 16)
					| ((hash[desplazamiento + 2] & 0xff) << 8) | (hash[desplazamiento + 3] & 0xff);
			return String.format("%0" + DIGITOS + "d", binario % MODULO);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular el código TOTP", e);
		}
	}
}