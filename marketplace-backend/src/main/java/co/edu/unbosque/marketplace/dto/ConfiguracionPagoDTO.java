package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * Lo que el frontend necesita para cobrar: si los pagos van a Stripe (en modo
 * de prueba) o al simulador, la clave publicable para Stripe Elements, la
 * moneda y las tarjetas de prueba con su resultado.
 */
public class ConfiguracionPagoDTO {
	/** STRIPE_TEST o SIMULADO. */
	private String modo;
	/** Clave publicable de Stripe (pk_test_...); null en modo SIMULADO. */
	private String clavePublica;

	/** Moneda en que se cobra (COP). */
	private String moneda;

	/** Tarjetas de prueba disponibles y qué resultado da cada una. */
	private List<MetodoPagoPruebaDTO> metodosPrueba;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ConfiguracionPagoDTO() {
	}

	/**
	 * Devuelve STRIPE_TEST o SIMULADO.
	 *
	 * @return STRIPE_TEST o SIMULADO
	 */
	public String getModo() {
		return modo;
	}

	/**
	 * Asigna STRIPE_TEST o SIMULADO.
	 *
	 * @param modo STRIPE_TEST o SIMULADO
	 */
	public void setModo(String modo) {
		this.modo = modo;
	}

	/**
	 * Devuelve la clave publicable de Stripe (pk_test_...); null en modo SIMULADO.
	 *
	 * @return clave publicable de Stripe (pk_test_...); null en modo SIMULADO
	 */
	public String getClavePublica() {
		return clavePublica;
	}

	/**
	 * Asigna la clave publicable de Stripe (pk_test_...); null en modo SIMULADO.
	 *
	 * @param clavePublica clave publicable de Stripe (pk_test_...); null en modo SIMULADO
	 */
	public void setClavePublica(String clavePublica) {
		this.clavePublica = clavePublica;
	}

	/**
	 * Devuelve la moneda en que se cobra (COP).
	 *
	 * @return moneda en que se cobra (COP)
	 */
	public String getMoneda() {
		return moneda;
	}

	/**
	 * Asigna la moneda en que se cobra (COP).
	 *
	 * @param moneda moneda en que se cobra (COP)
	 */
	public void setMoneda(String moneda) {
		this.moneda = moneda;
	}

	/**
	 * Devuelve las tarjetas de prueba disponibles y qué resultado da cada una.
	 *
	 * @return tarjetas de prueba disponibles y qué resultado da cada una
	 */
	public List<MetodoPagoPruebaDTO> getMetodosPrueba() {
		return metodosPrueba;
	}

	/**
	 * Asigna las tarjetas de prueba disponibles y qué resultado da cada una.
	 *
	 * @param metodosPrueba tarjetas de prueba disponibles y qué resultado da cada una
	 */
	public void setMetodosPrueba(List<MetodoPagoPruebaDTO> metodosPrueba) {
		this.metodosPrueba = metodosPrueba;
	}
}
