package co.edu.unbosque.marketplace.exception;

/**
 * Se lanza cuando no se puede guardar una imagen en Cloudinary, ya sea porque
 * falta la configuración ({@code CLOUDINARY_URL}) o porque el servicio no
 * respondió.
 * <p>
 * El {@link ManejadorGlobalExcepciones} la convierte en un 503. Como es una
 * excepción no verificada, la transacción en curso se revierte y no quedan
 * registros de imágenes que no se alcanzaron a subir.
 * </p>
 */
public class AlmacenamientoNoDisponibleException extends RuntimeException {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/**
	 * Crea la excepción con el mensaje que verá el usuario.
	 *
	 * @param mensaje explicación del problema
	 */
	public AlmacenamientoNoDisponibleException(String mensaje) {
		super(mensaje);
	}
}
