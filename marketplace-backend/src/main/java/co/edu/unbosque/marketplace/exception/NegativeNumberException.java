package co.edu.unbosque.marketplace.exception;

/**
 * Clase la cual es publica para ser llamada dentro de otras clases la cual es
 * heredada de Exception
 */
public class NegativeNumberException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NegativeNumberException() {
		super("No puede ser un numero negativo.");
	}

}
