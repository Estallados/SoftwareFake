package co.edu.unbosque.marketplace.dto;

/**
 * Método de pago de prueba de Stripe (PaymentMethod {@code pm_card_...}) con el
 * resultado que produce, para mostrarlo en el front o probar en Swagger.
 */
public class MetodoPagoPruebaDTO {

	/** Id del método de pago, por ejemplo {@code pm_card_visa}. */
	private String metodo;

	/** Descripción para el usuario, por ejemplo "Visa aprobada". */
	private String descripcion;

	/** Resultado que produce: APROBADO, RECHAZADO o ERROR. */
	private String resultado;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public MetodoPagoPruebaDTO() {
	}

	/**
	 * Crea un método de pago de prueba.
	 *
	 * @param metodo      id del método de pago
	 * @param descripcion descripción para el usuario
	 * @param resultado   resultado que produce
	 */
	public MetodoPagoPruebaDTO(String metodo, String descripcion, String resultado) {
		this.metodo = metodo;
		this.descripcion = descripcion;
		this.resultado = resultado;
	}

	/**
	 * Devuelve el id del método de pago, por ejemplo {@code pm_card_visa}.
	 *
	 * @return id del método de pago, por ejemplo {@code pm_card_visa}
	 */
	public String getMetodo() {
		return metodo;
	}

	/**
	 * Asigna el id del método de pago, por ejemplo {@code pm_card_visa}.
	 *
	 * @param metodo id del método de pago, por ejemplo {@code pm_card_visa}
	 */
	public void setMetodo(String metodo) {
		this.metodo = metodo;
	}

	/**
	 * Devuelve la descripción para el usuario, por ejemplo "Visa aprobada".
	 *
	 * @return descripción para el usuario, por ejemplo "Visa aprobada"
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna la descripción para el usuario, por ejemplo "Visa aprobada".
	 *
	 * @param descripcion descripción para el usuario, por ejemplo "Visa aprobada"
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	/**
	 * Devuelve el resultado que produce: APROBADO, RECHAZADO o ERROR.
	 *
	 * @return resultado que produce: APROBADO, RECHAZADO o ERROR
	 */
	public String getResultado() {
		return resultado;
	}

	/**
	 * Asigna el resultado que produce: APROBADO, RECHAZADO o ERROR.
	 *
	 * @param resultado resultado que produce: APROBADO, RECHAZADO o ERROR
	 */
	public void setResultado(String resultado) {
		this.resultado = resultado;
	}
}
