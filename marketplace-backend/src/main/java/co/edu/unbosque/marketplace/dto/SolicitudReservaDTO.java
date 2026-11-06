package co.edu.unbosque.marketplace.dto;

/**
 * Solicitud para crear una reserva desde una cotización vigente y pagarla (pago
 * de prueba).
 */
public class SolicitudReservaDTO {

	/** Cotización vigente con la que se crea la reserva. */
	private Long idCotizacion;

	/** Método de pago de prueba, por ejemplo {@code pm_card_visa}. */
	private String metodoPago;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public SolicitudReservaDTO() {
	}

	/**
	 * Devuelve la cotización vigente con la que se crea la reserva.
	 *
	 * @return cotización vigente con la que se crea la reserva
	 */
	public Long getIdCotizacion() {
		return idCotizacion;
	}

	/**
	 * Asigna la cotización vigente con la que se crea la reserva.
	 *
	 * @param idCotizacion cotización vigente con la que se crea la reserva
	 */
	public void setIdCotizacion(Long idCotizacion) {
		this.idCotizacion = idCotizacion;
	}

	/**
	 * Devuelve el método de pago de prueba, por ejemplo {@code pm_card_visa}.
	 *
	 * @return método de pago de prueba, por ejemplo {@code pm_card_visa}
	 */
	public String getMetodoPago() {
		return metodoPago;
	}

	/**
	 * Asigna el método de pago de prueba, por ejemplo {@code pm_card_visa}.
	 *
	 * @param metodoPago método de pago de prueba, por ejemplo {@code pm_card_visa}
	 */
	public void setMetodoPago(String metodoPago) {
		this.metodoPago = metodoPago;
	}
}
