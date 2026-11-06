package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Mensaje de una conversación entre un huésped y un anfitrión.
 */
public class MensajeDTO {
	/** Identificador del mensaje. */
	private Long idMensaje;

	/** Texto del mensaje. */
	private String contenido;

	/** Momento en que se envió. */
	private LocalDateTime fechaEnvio;

	/** Conversación a la que pertenece. */
	private Long idConversacion;

	/** Usuario que lo envió. */
	private Long idUsuario;
	/** Nombre de quien envió el mensaje. */
	private String nombreUsuario;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public MensajeDTO() {
	}

	/**
	 * Devuelve el identificador del mensaje.
	 *
	 * @return identificador del mensaje
	 */
	public Long getIdMensaje() {
		return idMensaje;
	}

	/**
	 * Asigna el identificador del mensaje.
	 *
	 * @param idMensaje identificador del mensaje
	 */
	public void setIdMensaje(Long idMensaje) {
		this.idMensaje = idMensaje;
	}

	/**
	 * Devuelve el texto del mensaje.
	 *
	 * @return texto del mensaje
	 */
	public String getContenido() {
		return contenido;
	}

	/**
	 * Asigna el texto del mensaje.
	 *
	 * @param contenido texto del mensaje
	 */
	public void setContenido(String contenido) {
		this.contenido = contenido;
	}

	/**
	 * Devuelve el momento en que se envió.
	 *
	 * @return momento en que se envió
	 */
	public LocalDateTime getFechaEnvio() {
		return fechaEnvio;
	}

	/**
	 * Asigna el momento en que se envió.
	 *
	 * @param fechaEnvio momento en que se envió
	 */
	public void setFechaEnvio(LocalDateTime fechaEnvio) {
		this.fechaEnvio = fechaEnvio;
	}

	/**
	 * Devuelve la conversación a la que pertenece.
	 *
	 * @return conversación a la que pertenece
	 */
	public Long getIdConversacion() {
		return idConversacion;
	}

	/**
	 * Asigna la conversación a la que pertenece.
	 *
	 * @param idConversacion conversación a la que pertenece
	 */
	public void setIdConversacion(Long idConversacion) {
		this.idConversacion = idConversacion;
	}

	/**
	 * Devuelve el usuario que lo envió.
	 *
	 * @return usuario que lo envió
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que lo envió.
	 *
	 * @param idUsuario usuario que lo envió
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el nombre de quien envió el mensaje.
	 *
	 * @return nombre de quien envió el mensaje
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna el nombre de quien envió el mensaje.
	 *
	 * @param nombreUsuario nombre de quien envió el mensaje
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}
}
