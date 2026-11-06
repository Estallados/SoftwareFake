package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Período en que un alojamiento se ofrece para reservar: desde fechaInicio
 * (primer día de llegada) hasta fechaFin (último día de salida).
 */
public class PeriodoDisponibilidadDTO {
	/** Identificador del período. */
	private Long idPeriodo;

	/** Alojamiento al que pertenece. */
	private Long idAlojamiento;

	/** Primer día en que se puede llegar. */
	private LocalDate fechaInicio;

	/** Último día en que se puede salir. */
	private LocalDate fechaFin;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public PeriodoDisponibilidadDTO() {
	}

	/**
	 * Devuelve el identificador del período.
	 *
	 * @return identificador del período
	 */
	public Long getIdPeriodo() {
		return idPeriodo;
	}

	/**
	 * Asigna el identificador del período.
	 *
	 * @param idPeriodo identificador del período
	 */
	public void setIdPeriodo(Long idPeriodo) {
		this.idPeriodo = idPeriodo;
	}

	/**
	 * Devuelve el alojamiento al que pertenece.
	 *
	 * @return alojamiento al que pertenece
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento al que pertenece.
	 *
	 * @param idAlojamiento alojamiento al que pertenece
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el primer día en que se puede llegar.
	 *
	 * @return primer día en que se puede llegar
	 */
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}

	/**
	 * Asigna el primer día en que se puede llegar.
	 *
	 * @param fechaInicio primer día en que se puede llegar
	 */
	public void setFechaInicio(LocalDate fechaInicio) {
		this.fechaInicio = fechaInicio;
	}

	/**
	 * Devuelve el último día en que se puede salir.
	 *
	 * @return último día en que se puede salir
	 */
	public LocalDate getFechaFin() {
		return fechaFin;
	}

	/**
	 * Asigna el último día en que se puede salir.
	 *
	 * @param fechaFin último día en que se puede salir
	 */
	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
	}
}
