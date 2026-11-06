package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoConversacion;

/**
 * Conversación entre un huésped (idUsuario) y un anfitrión, con sus nombres.
 * Para iniciarla, el huésped manda idAlojamiento y, si quiere, un primer
 * mensaje.
 */
public class ConversacionDTO {
	/** Identificador de la conversación. */
	private Long idConversacion;

	/** Momento en que se inició. */
	private LocalDateTime fechaCreacion;

	/** Estado de la conversación. */
	private EstadoConversacion estado;

	/** Huésped de la conversación. */
	private Long idUsuario;

	/** Anfitrión de la conversación. */
	private Long idAnfitrion;

	/** Alojamiento sobre el que se conversa. */
	private Long idAlojamiento;

	/** Primer mensaje (solo al iniciarla). */
	private String mensaje;

	/** Nombre del huésped. */
	private String nombreUsuario;

	/** Nombre del anfitrión. */
	private String nombreAnfitrion;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ConversacionDTO() {
	}

	/**
	 * Devuelve el identificador de la conversación.
	 *
	 * @return identificador de la conversación
	 */
	public Long getIdConversacion() {
		return idConversacion;
	}

	/**
	 * Asigna el identificador de la conversación.
	 *
	 * @param idConversacion identificador de la conversación
	 */
	public void setIdConversacion(Long idConversacion) {
		this.idConversacion = idConversacion;
	}

	/**
	 * Devuelve el momento en que se inició.
	 *
	 * @return momento en que se inició
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que se inició.
	 *
	 * @param fechaCreacion momento en que se inició
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	/**
	 * Devuelve el estado de la conversación.
	 *
	 * @return estado de la conversación
	 */
	public EstadoConversacion getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado de la conversación.
	 *
	 * @param estado estado de la conversación
	 */
	public void setEstado(EstadoConversacion estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el huésped de la conversación.
	 *
	 * @return huésped de la conversación
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el huésped de la conversación.
	 *
	 * @param idUsuario huésped de la conversación
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el anfitrión de la conversación.
	 *
	 * @return anfitrión de la conversación
	 */
	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	/**
	 * Asigna el anfitrión de la conversación.
	 *
	 * @param idAnfitrion anfitrión de la conversación
	 */
	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	/**
	 * Devuelve el alojamiento sobre el que se conversa.
	 *
	 * @return alojamiento sobre el que se conversa
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento sobre el que se conversa.
	 *
	 * @param idAlojamiento alojamiento sobre el que se conversa
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el primer mensaje (solo al iniciarla).
	 *
	 * @return primer mensaje (solo al iniciarla)
	 */
	public String getMensaje() {
		return mensaje;
	}

	/**
	 * Asigna el primer mensaje (solo al iniciarla).
	 *
	 * @param mensaje primer mensaje (solo al iniciarla)
	 */
	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	/**
	 * Devuelve el nombre del huésped.
	 *
	 * @return nombre del huésped
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna el nombre del huésped.
	 *
	 * @param nombreUsuario nombre del huésped
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	/**
	 * Devuelve el nombre del anfitrión.
	 *
	 * @return nombre del anfitrión
	 */
	public String getNombreAnfitrion() {
		return nombreAnfitrion;
	}

	/**
	 * Asigna el nombre del anfitrión.
	 *
	 * @param nombreAnfitrion nombre del anfitrión
	 */
	public void setNombreAnfitrion(String nombreAnfitrion) {
		this.nombreAnfitrion = nombreAnfitrion;
	}
}
