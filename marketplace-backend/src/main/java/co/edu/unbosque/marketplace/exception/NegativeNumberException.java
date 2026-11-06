package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando un valor numérico que no puede ser negativo es menor que
 * cero.
 * <p>
 * La usa {@link ExceptionCheker#checkerNegativeNumber(int)}.
 * </p>
 */
public class NegativeNumberException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public NegativeNumberException() {
		super("No puede ser un numero negativo.");
	}

}
