package co.edu.unbosque.marketplace.dto;

/**
 * Solicitud para crear una reserva desde una cotización vigente y pagarla (pago
 * de prueba).
 *
 * @see "RF-021, RF-028"
 */
public class SolicitudReservaDTO {
	private Long idCotizacion;
	private String metodoPago;

	public SolicitudReservaDTO() {
	}

	public Long getIdCotizacion() {
		return idCotizacion;
	}

	public void setIdCotizacion(Long idCotizacion) {
		this.idCotizacion = idCotizacion;
	}

	public String getMetodoPago() {
		return metodoPago;
	}

	public void setMetodoPago(String metodoPago) {
		this.metodoPago = metodoPago;
	}
}
