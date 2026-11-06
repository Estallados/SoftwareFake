package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

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
 * Mensaje dentro de una conversación entre un huésped y un anfitrión: quién lo
 * envió ({@code idUsuario}), qué dice y cuándo se envió.
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {

	/** Identificador del mensaje. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idMensaje;

	/** Texto del mensaje. */
	private String contenido;

	/** Momento en que se envió. */
	private LocalDateTime fechaEnvio;

	/** Conversación a la que pertenece. */
	@Column(name = "id_conversacion")
	private Long idConversacion;

	/**
	 * Clave foránea hacia {@link Conversacion}; el valor se asigna con
	 * {@code idConversacion}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_conversacion", insertable = false, updatable = false)
	private Conversacion conversacion;

	/** Usuario que envió el mensaje. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Constructor vacío, requerido por JPA. */
	public Mensaje() {
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
	 * Devuelve el usuario que envió el mensaje.
	 *
	 * @return usuario que envió el mensaje
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que envió el mensaje.
	 *
	 * @param idUsuario usuario que envió el mensaje
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}
}
