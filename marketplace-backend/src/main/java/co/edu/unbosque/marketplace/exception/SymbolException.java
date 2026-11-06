package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando una contraseña no tiene ningún símbolo (un carácter que no
 * sea letra ni número).
 * <p>
 * La usa {@link ExceptionCheker#checkerSymbol(String)} como parte de la
 * validación de contraseñas.
 * </p>
 */
public class SymbolException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public SymbolException() {
		super("Debe contener al menos un simbolo.");
	}

}
