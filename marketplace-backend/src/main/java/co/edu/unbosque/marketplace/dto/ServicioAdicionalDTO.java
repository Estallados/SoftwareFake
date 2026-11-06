package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

/**
 * Servicio adicional que ofrece un alojamiento (desayuno, traslado, etc.), tal
 * como entra y sale por la API.
 */
public class ServicioAdicionalDTO {

	/** Identificador del servicio. */
	private Long idServicio;

	/** Nombre del servicio. */
	private String nombre;

	/** Costo por reserva, en pesos colombianos. */
	private BigDecimal costo;

	/** Alojamiento que ofrece el servicio. */
	private Long idAlojamiento;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ServicioAdicionalDTO() {
	}

	/**
	 * Devuelve el identificador del servicio.
	 *
	 * @return identificador del servicio
	 */
	public Long getIdServicio() {
		return idServicio;
	}

	/**
	 * Asigna el identificador del servicio.
	 *
	 * @param idServicio identificador del servicio
	 */
	public void setIdServicio(Long idServicio) {
		this.idServicio = idServicio;
	}

	/**
	 * Devuelve el nombre del servicio.
	 *
	 * @return nombre del servicio
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre del servicio.
	 *
	 * @param nombre nombre del servicio
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve el costo por reserva, en pesos colombianos.
	 *
	 * @return costo por reserva, en pesos colombianos
	 */
	public BigDecimal getCosto() {
		return costo;
	}

	/**
	 * Asigna el costo por reserva, en pesos colombianos.
	 *
	 * @param costo costo por reserva, en pesos colombianos
	 */
	public void setCosto(BigDecimal costo) {
		this.costo = costo;
	}

	/**
	 * Devuelve el alojamiento que ofrece el servicio.
	 *
	 * @return alojamiento que ofrece el servicio
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento que ofrece el servicio.
	 *
	 * @param idAlojamiento alojamiento que ofrece el servicio
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}
}
