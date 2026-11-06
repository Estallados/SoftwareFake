package co.edu.unbosque.marketplace.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Período en el que el anfitrión ofrece su alojamiento.
 * <p>
 * Una estancia [llegada, salida) cabe en el período si la llegada es igual o
 * posterior a {@code fechaInicio} y la salida es igual o anterior a
 * {@code fechaFin}. Las fechas ya reservadas no se guardan aquí: se calculan a
 * partir de las reservas activas.
 * </p>
 */
@Entity
@Table(name = "periodoDisponibilidad")
public class PeriodoDisponibilidad {

	/** Identificador del período. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idPeriodo;

	/** Alojamiento al que pertenece el período. */
	@Column(name = "id_alojamiento")
	private Long idAlojamiento;

	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;

	/** Primer día en que se puede llegar. */
	private LocalDate fechaInicio;

	/** Último día en que se puede salir (la salida no ocupa noche). */
	private LocalDate fechaFin;

	/** Constructor vacío, requerido por JPA. */
	public PeriodoDisponibilidad() {
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
	 * Devuelve el alojamiento al que pertenece el período.
	 *
	 * @return alojamiento al que pertenece el período
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento al que pertenece el período.
	 *
	 * @param idAlojamiento alojamiento al que pertenece el período
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
	 * Devuelve el último día en que se puede salir (la salida no ocupa noche).
	 *
	 * @return último día en que se puede salir (la salida no ocupa noche)
	 */
	public LocalDate getFechaFin() {
		return fechaFin;
	}

	/**
	 * Asigna el último día en que se puede salir (la salida no ocupa noche).
	 *
	 * @param fechaFin último día en que se puede salir (la salida no ocupa noche)
	 */
	public void setFechaFin(LocalDate fechaFin) {
		this.fechaFin = fechaFin;
	}
}
