package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Notificación dentro de la plataforma, tal como la ve el usuario en la
 * campana.
 */
public class NotificacionDTO {
	/** Identificador de la notificación. */
	private Long idNotificacion;

	/** Evento que la originó, por ejemplo RESERVA_CONFIRMADA. */
	private String tipo;

	/** Texto que ve el usuario. */
	private String mensaje;

	/** Momento en que se generó. */
	private LocalDateTime fecha;

	/** Indica si el usuario ya la leyó. */
	private Boolean leida;

	/** Usuario que la recibe. */
	private Long idUsuario;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public NotificacionDTO() {
	}

	/**
	 * Devuelve el identificador de la notificación.
	 *
	 * @return identificador de la notificación
	 */
	public Long getIdNotificacion() {
		return idNotificacion;
	}

	/**
	 * Asigna el identificador de la notificación.
	 *
	 * @param idNotificacion identificador de la notificación
	 */
	public void setIdNotificacion(Long idNotificacion) {
		this.idNotificacion = idNotificacion;
	}

	/**
	 * Devuelve el evento que la originó, por ejemplo RESERVA_CONFIRMADA.
	 *
	 * @return evento que la originó, por ejemplo RESERVA_CONFIRMADA
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Asigna el evento que la originó, por ejemplo RESERVA_CONFIRMADA.
	 *
	 * @param tipo evento que la originó, por ejemplo RESERVA_CONFIRMADA
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Devuelve el texto que ve el usuario.
	 *
	 * @return texto que ve el usuario
	 */
	public String getMensaje() {
		return mensaje;
	}

	/**
	 * Asigna el texto que ve el usuario.
	 *
	 * @param mensaje texto que ve el usuario
	 */
	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	/**
	 * Devuelve el momento en que se generó.
	 *
	 * @return momento en que se generó
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento en que se generó.
	 *
	 * @param fecha momento en que se generó
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve {@code true} si el usuario ya la leyó.
	 *
	 * @return {@code true} si el usuario ya la leyó
	 */
	public Boolean getLeida() {
		return leida;
	}

	/**
	 * Asigna {@code true} si el usuario ya la leyó.
	 *
	 * @param leida {@code true} si el usuario ya la leyó
	 */
	public void setLeida(Boolean leida) {
		this.leida = leida;
	}

	/**
	 * Devuelve el usuario que la recibe.
	 *
	 * @return usuario que la recibe
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que la recibe.
	 *
	 * @param idUsuario usuario que la recibe
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}
}
