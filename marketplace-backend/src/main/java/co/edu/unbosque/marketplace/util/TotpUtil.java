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
	private static final SecureRandom RANDOM = new SecureRandom();

	private TotpUtil() {
		throw new IllegalStateException("Clase utilitaria");
	}

	/** Genera un secreto nuevo en Base32 (sin relleno). */
	public static String generarSecreto() {
		byte[] bytes = new byte[BYTES_SECRETO];
		RANDOM.nextBytes(bytes);
		return new Base32().encodeToString(bytes).replace("=", "");
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
		for (int i = -TOLERANCIA_PASOS; i <= TOLERANCIA_PASOS; i++) {
			if (MessageDigest.isEqual(codigoPara(secreto, pasoActual + i).getBytes(), codigo.getBytes())) {
				return true;
			}
		}
		return false;
	}

	private static String codigoPara(String secreto, long paso) {
		try {
			byte[] clave = new Base32().decode(secreto);
			Mac mac = Mac.getInstance("HmacSHA1");
			// HMAC-SHA1 se utiliza para mantener la compatibilidad con RFC 6238.
			mac.init(new SecretKeySpec(clave, "HmacSHA1"));
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
