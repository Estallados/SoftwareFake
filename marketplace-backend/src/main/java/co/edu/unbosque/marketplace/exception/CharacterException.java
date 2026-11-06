package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando una contraseña no tiene un largo válido (debe tener entre 8
 * y 64 caracteres).
 * <p>
 * La usa {@link ExceptionCheker#checkerCharacter(String)} como parte de la
 * validación de contraseñas.
 * </p>
 */
public class CharacterException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public CharacterException() {
		super("Debe contener mas de 8 caracteres.");
	}

}
