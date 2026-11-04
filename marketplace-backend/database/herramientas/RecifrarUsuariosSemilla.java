import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Genera los UPDATE que adaptan los usuarios de 05_seed_data.sql a tu AES_SECRET_KEY,
 * con el mismo algoritmo de AESUtil. Solo lee la variable de entorno e imprime SQL: no se
 * conecta a nada. No necesita compilar el proyecto:
 *
 * <pre>
 *   java RecifrarUsuariosSemilla.java > recifrar.sql
 *   psql -v ON_ERROR_STOP=1 -d marketplace -f recifrar.sql
 * </pre>
 *
 * El archivo generado contiene los correos cifrados con tu clave, no la clave.
 */
public class RecifrarUsuariosSemilla {

	private static final int TAMANO_IV = 12;
	private static final int TAMANO_TAG = 128;

	/** Mismos ids y correos de 05_seed_data.sql; el índice + 1 es el id_usuario. */
	private static final String[] CORREOS = { "admin@marketplace.local", "laura.gomez@marketplace.test",
			"carlos.ruiz@marketplace.test", "ana.torres@marketplace.test", "diego.mora@marketplace.test",
			"sofia.perez@marketplace.test", "pedro.diaz@marketplace.test" };

	public static void main(String[] args) throws Exception {
		String valor = System.getenv("AES_SECRET_KEY");
		if (valor == null || valor.isBlank()) {
			System.err.println("Define la variable de entorno AES_SECRET_KEY antes de ejecutar.");
			System.exit(1);
		}
		byte[] clave = Base64.getDecoder().decode(valor);

		System.out.println("BEGIN;");
		for (int i = 0; i < CORREOS.length; i++) {
			System.out.printf("UPDATE usuario SET correo = '%s', correo_hash = '%s' WHERE id_usuario = %d;%n",
					cifrar(CORREOS[i], clave), hash(CORREOS[i], clave), i + 1);
		}
		System.out.println("COMMIT;");
	}

	static String cifrar(String texto, byte[] clave) throws Exception {
		byte[] iv = new byte[TAMANO_IV];
		new SecureRandom().nextBytes(iv);
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(TAMANO_TAG, iv));
		byte[] cifrado = cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8));
		byte[] salida = new byte[iv.length + cifrado.length];
		System.arraycopy(iv, 0, salida, 0, iv.length);
		System.arraycopy(cifrado, 0, salida, iv.length, cifrado.length);
		return Base64.getEncoder().encodeToString(salida);
	}

	static String hash(String texto, byte[] clave) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(clave, "HmacSHA256"));
		return Base64.getEncoder().encodeToString(mac.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
	}
}
