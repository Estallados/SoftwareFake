package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando un correo no tiene el formato de una dirección de correo
 * (usuario, arroba, dominio y extensión).
 * <p>
 * La usa {@link ExceptionCheker#checkerMail(String)}.
 * </p>
 */
public class MailException extends Exception {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que se le muestra al usuario.
	 */
	public MailException() {
		super("Debe de tener los parametros necesarios de un correo");
	}

}
