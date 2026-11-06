package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;

/**
 * Evento de la auditoría, tal como lo consulta el administrador.
 */
public class EventoAuditoriaDTO {
	/** Identificador del evento. */
	private Long idEvento;

	/** Acción realizada, por ejemplo LOGIN. */
	private String accion;

	/** Momento del evento. */
	private LocalDateTime fecha;

	/** Información adicional del evento. */
	private String detalle;

	/** Usuario que hizo la acción; {@code null} si no se conoce. */
	private Long idUsuario;

	/** Tipo de recurso afectado, por ejemplo USUARIO. */
	private String entidad;

	/** Id del recurso afectado. */
	private Long idEntidad;

	/** Resultado de la operación. */
	private ResultadoAuditoria resultado;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public EventoAuditoriaDTO() {
	}

	/**
	 * Devuelve el identificador del evento.
	 *
	 * @return identificador del evento
	 */
	public Long getIdEvento() {
		return idEvento;
	}

	/**
	 * Asigna el identificador del evento.
	 *
	 * @param idEvento identificador del evento
	 */
	public void setIdEvento(Long idEvento) {
		this.idEvento = idEvento;
	}

	/**
	 * Devuelve la acción realizada, por ejemplo LOGIN.
	 *
	 * @return acción realizada, por ejemplo LOGIN
	 */
	public String getAccion() {
		return accion;
	}

	/**
	 * Asigna la acción realizada, por ejemplo LOGIN.
	 *
	 * @param accion acción realizada, por ejemplo LOGIN
	 */
	public void setAccion(String accion) {
		this.accion = accion;
	}

	/**
	 * Devuelve el momento del evento.
	 *
	 * @return momento del evento
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del evento.
	 *
	 * @param fecha momento del evento
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve la información adicional del evento.
	 *
	 * @return información adicional del evento
	 */
	public String getDetalle() {
		return detalle;
	}

	/**
	 * Asigna la información adicional del evento.
	 *
	 * @param detalle información adicional del evento
	 */
	public void setDetalle(String detalle) {
		this.detalle = detalle;
	}

	/**
	 * Devuelve el usuario que hizo la acción; {@code null} si no se conoce.
	 *
	 * @return usuario que hizo la acción; {@code null} si no se conoce
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que hizo la acción; {@code null} si no se conoce.
	 *
	 * @param idUsuario usuario que hizo la acción; {@code null} si no se conoce
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el tipo de recurso afectado, por ejemplo USUARIO.
	 *
	 * @return tipo de recurso afectado, por ejemplo USUARIO
	 */
	public String getEntidad() {
		return entidad;
	}

	/**
	 * Asigna el tipo de recurso afectado, por ejemplo USUARIO.
	 *
	 * @param entidad tipo de recurso afectado, por ejemplo USUARIO
	 */
	public void setEntidad(String entidad) {
		this.entidad = entidad;
	}

	/**
	 * Devuelve el id del recurso afectado.
	 *
	 * @return id del recurso afectado
	 */
	public Long getIdEntidad() {
		return idEntidad;
	}

	/**
	 * Asigna el id del recurso afectado.
	 *
	 * @param idEntidad id del recurso afectado
	 */
	public void setIdEntidad(Long idEntidad) {
		this.idEntidad = idEntidad;
	}

	/**
	 * Devuelve el resultado de la operación.
	 *
	 * @return resultado de la operación
	 */
	public ResultadoAuditoria getResultado() {
		return resultado;
	}

	/**
	 * Asigna el resultado de la operación.
	 *
	 * @param resultado resultado de la operación
	 */
	public void setResultado(ResultadoAuditoria resultado) {
		this.resultado = resultado;
	}
}
