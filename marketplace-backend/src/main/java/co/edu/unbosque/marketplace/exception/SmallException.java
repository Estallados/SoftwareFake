package co.edu.unbosque.marketplace.exception;

/**
 * Clase la cual es publica para ser llamada dentro de otras clases la cual es
 * heredada de Exception
 *
 * @see "RNF-008"
 */
public class SmallException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Se crea un constructor el cual no recibe ningun tipo de parametro se llama el
	 * super como constructor de la clase madre recibiendo un texto.
	 */
	public SmallException() {
		super("Debe contener al menos una letra minúscula.");
	}

}
