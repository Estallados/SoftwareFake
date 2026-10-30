package co.edu.unbosque.marketplace.exception;

/**
 * No se pudo guardar una imagen en Cloudinary (falta la configuración o el
 * servicio no respondió). Se responde 503 y, como es una excepción no
 * verificada, la transacción en curso se revierte.
 *
 * @see "RF-017"
 */
public class AlmacenamientoNoDisponibleException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AlmacenamientoNoDisponibleException(String mensaje) {
		super(mensaje);
	}
}
