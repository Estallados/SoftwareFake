package co.edu.unbosque.marketplace.dto;

/**
 * Método de pago de prueba de Stripe (PaymentMethod {@code pm_card_...}) con el
 * resultado que produce, para mostrarlo en el front o probar en Swagger.
 *
 * @see "RF-028"
 */
public class MetodoPagoPruebaDTO {
	private String metodo;
	private String descripcion;
	/** APROBADO, RECHAZADO o ERROR. */
	private String resultado;

	public MetodoPagoPruebaDTO() {
	}

	public MetodoPagoPruebaDTO(String metodo, String descripcion, String resultado) {
		this.metodo = metodo;
		this.descripcion = descripcion;
		this.resultado = resultado;
	}

	public String getMetodo() {
		return metodo;
	}

	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getResultado() {
		return resultado;
	}

	public void setResultado(String resultado) {
		this.resultado = resultado;
	}
}
