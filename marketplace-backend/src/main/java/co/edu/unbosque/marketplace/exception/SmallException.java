package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando una contraseña no tiene ninguna letra minúscula.
 * <p>
 * La usa {@link ExceptionCheker#checkerSmall(String)} como parte de la
 * validación de contraseñas.
 * </p>
 */
public class SmallException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public SmallException() {
		super("Debe contener al menos una letra minúscula.");
	}

}
