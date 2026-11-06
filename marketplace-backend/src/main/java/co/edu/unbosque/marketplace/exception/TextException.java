package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando un texto que solo debe tener letras (con tildes y ñ) trae
 * números, espacios u otros caracteres.
 * <p>
 * La usa {@link ExceptionCheker#checkerText(String)}.
 * </p>
 */
public class TextException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public TextException() {
		super("Solo puede contener letras.");
	}
}
