package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Resultado de consultar si un alojamiento está libre en unas fechas. Si no lo
 * está, motivo explica por qué (fechas fuera de los períodos, ya reservadas o
 * publicación no activa).
 */
public class ConsultaDisponibilidadDTO {
	/** Alojamiento consultado. */
	private Long idAlojamiento;

	/** Día de llegada consultado. */
	private LocalDate fechaEntrada;

	/** Día de salida consultado. */
	private LocalDate fechaSalida;

	/** Indica si se puede reservar en esas fechas. */
	private Boolean disponible;

	/** Por qué no está disponible; {@code null} si sí lo está. */
	private String motivo;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ConsultaDisponibilidadDTO() {
	}

	/**
	 * Devuelve el alojamiento consultado.
	 *
	 * @return alojamiento consultado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento consultado.
	 *
	 * @param idAlojamiento alojamiento consultado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el día de llegada consultado.
	 *
	 * @return día de llegada consultado
	 */
	public LocalDate getFechaEntrada() {
		return fechaEntrada;
	}

	/**
	 * Asigna el día de llegada consultado.
	 *
	 * @param fechaEntrada día de llegada consultado
	 */
	public void setFechaEntrada(LocalDate fechaEntrada) {
		this.fechaEntrada = fechaEntrada;
	}

	/**
	 * Devuelve el día de salida consultado.
	 *
	 * @return día de salida consultado
	 */
	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	/**
	 * Asigna el día de salida consultado.
	 *
	 * @param fechaSalida día de salida consultado
	 */
	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}

	/**
	 * Devuelve {@code true} si se puede reservar en esas fechas.
	 *
	 * @return {@code true} si se puede reservar en esas fechas
	 */
	public Boolean getDisponible() {
		return disponible;
	}

	/**
	 * Asigna {@code true} si se puede reservar en esas fechas.
	 *
	 * @param disponible {@code true} si se puede reservar en esas fechas
	 */
	public void setDisponible(Boolean disponible) {
		this.disponible = disponible;
	}

	/**
	 * Devuelve por qué no está disponible; {@code null} si sí lo está.
	 *
	 * @return por qué no está disponible; {@code null} si sí lo está
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna por qué no está disponible; {@code null} si sí lo está.
	 *
	 * @param motivo por qué no está disponible; {@code null} si sí lo está
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
