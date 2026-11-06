package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

/**
 * Un cambio de estado de un alojamiento: de qué estado a cuál, por qué, quién
 * lo hizo y cuándo.
 */
public class HistorialEstadoAlojamientoDTO {
	/** Identificador del registro. */
	private Long idHistorial;

	/** Alojamiento que cambió de estado. */
	private Long idAlojamiento;

	/** Estado antes del cambio; {@code null} al crearlo. */
	private EstadoAlojamiento estadoAnterior;

	/** Estado después del cambio. */
	private EstadoAlojamiento estadoNuevo;

	/** Motivo del cambio. */
	private String motivo;

	/** Usuario que hizo el cambio. */
	private Long idUsuario;

	/** Momento del cambio. */
	private LocalDateTime fecha;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public HistorialEstadoAlojamientoDTO() {
	}

	/**
	 * Devuelve el identificador del registro.
	 *
	 * @return identificador del registro
	 */
	public Long getIdHistorial() {
		return idHistorial;
	}

	/**
	 * Asigna el identificador del registro.
	 *
	 * @param idHistorial identificador del registro
	 */
	public void setIdHistorial(Long idHistorial) {
		this.idHistorial = idHistorial;
	}

	/**
	 * Devuelve el alojamiento que cambió de estado.
	 *
	 * @return alojamiento que cambió de estado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento que cambió de estado.
	 *
	 * @param idAlojamiento alojamiento que cambió de estado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el estado antes del cambio; {@code null} al crearlo.
	 *
	 * @return estado antes del cambio; {@code null} al crearlo
	 */
	public EstadoAlojamiento getEstadoAnterior() {
		return estadoAnterior;
	}

	/**
	 * Asigna el estado antes del cambio; {@code null} al crearlo.
	 *
	 * @param estadoAnterior estado antes del cambio; {@code null} al crearlo
	 */
	public void setEstadoAnterior(EstadoAlojamiento estadoAnterior) {
		this.estadoAnterior = estadoAnterior;
	}

	/**
	 * Devuelve el estado después del cambio.
	 *
	 * @return estado después del cambio
	 */
	public EstadoAlojamiento getEstadoNuevo() {
		return estadoNuevo;
	}

	/**
	 * Asigna el estado después del cambio.
	 *
	 * @param estadoNuevo estado después del cambio
	 */
	public void setEstadoNuevo(EstadoAlojamiento estadoNuevo) {
		this.estadoNuevo = estadoNuevo;
	}

	/**
	 * Devuelve el motivo del cambio.
	 *
	 * @return motivo del cambio
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna el motivo del cambio.
	 *
	 * @param motivo motivo del cambio
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	/**
	 * Devuelve el usuario que hizo el cambio.
	 *
	 * @return usuario que hizo el cambio
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que hizo el cambio.
	 *
	 * @param idUsuario usuario que hizo el cambio
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el momento del cambio.
	 *
	 * @return momento del cambio
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del cambio.
	 *
	 * @param fecha momento del cambio
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
