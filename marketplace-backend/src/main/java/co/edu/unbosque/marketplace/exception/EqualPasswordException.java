package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando la contraseña y su confirmación no coinciden.
 * <p>
 * La usa {@link ExceptionCheker#checkerEqualPassword(String, String)}.
 * </p>
 */
public class EqualPasswordException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public EqualPasswordException() {
		super("Las contraseñas deben coincidir.");
	}
}
