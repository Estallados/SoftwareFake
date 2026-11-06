package co.edu.unbosque.marketplace.dto;

import java.util.List;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

/**
 * Calendario de un alojamiento: los períodos que ofrece el anfitrión y los
 * rangos que ya están ocupados por reservas activas. Con esto el frontend
 * pinta qué días se pueden reservar.
 */
public class DisponibilidadDTO {
	/** Alojamiento consultado. */
	private Long idAlojamiento;

	/** Estado de la publicación; si no está ACTIVA no se puede reservar. */
	private EstadoAlojamiento estadoAlojamiento;

	/** Períodos ofrecidos por el anfitrión. */
	private List<PeriodoDisponibilidadDTO> periodos;

	/** Rangos ocupados por reservas activas. */
	private List<RangoReservadoDTO> reservados;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public DisponibilidadDTO() {
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
	 * Devuelve el estado de la publicación; si no está ACTIVA no se puede reservar.
	 *
	 * @return estado de la publicación; si no está ACTIVA no se puede reservar
	 */
	public EstadoAlojamiento getEstadoAlojamiento() {
		return estadoAlojamiento;
	}

	/**
	 * Asigna el estado de la publicación; si no está ACTIVA no se puede reservar.
	 *
	 * @param estadoAlojamiento estado de la publicación; si no está ACTIVA no se puede reservar
	 */
	public void setEstadoAlojamiento(EstadoAlojamiento estadoAlojamiento) {
		this.estadoAlojamiento = estadoAlojamiento;
	}

	/**
	 * Devuelve los períodos ofrecidos por el anfitrión.
	 *
	 * @return períodos ofrecidos por el anfitrión
	 */
	public List<PeriodoDisponibilidadDTO> getPeriodos() {
		return periodos;
	}

	/**
	 * Asigna los períodos ofrecidos por el anfitrión.
	 *
	 * @param periodos períodos ofrecidos por el anfitrión
	 */
	public void setPeriodos(List<PeriodoDisponibilidadDTO> periodos) {
		this.periodos = periodos;
	}

	/**
	 * Devuelve los rangos ocupados por reservas activas.
	 *
	 * @return rangos ocupados por reservas activas
	 */
	public List<RangoReservadoDTO> getReservados() {
		return reservados;
	}

	/**
	 * Asigna los rangos ocupados por reservas activas.
	 *
	 * @param reservados rangos ocupados por reservas activas
	 */
	public void setReservados(List<RangoReservadoDTO> reservados) {
		this.reservados = reservados;
	}
}
