package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando una contraseña no tiene ningún número.
 * <p>
 * La usa {@link ExceptionCheker#checkerNumber(String)} como parte de la
 * validación de contraseñas.
 * </p>
 */
public class NumberException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public NumberException() {
		super("Debe contener al menos un numero");
	}

}
