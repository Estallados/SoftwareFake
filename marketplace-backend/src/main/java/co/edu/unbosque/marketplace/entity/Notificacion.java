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
 * Notificación que recibe un usuario dentro de la plataforma (la campana del
 * menú), por ejemplo cuando le confirman una reserva o le bloquean un
 * alojamiento. También se envía en tiempo real por WebSocket.
 */
@Entity
@Table(name = "notificacion")
public class Notificacion {

	/** Identificador de la notificación. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idNotificacion;

	/** Evento que la originó, por ejemplo RESERVA_CONFIRMADA. */
	private String tipo;

	/** Texto que ve el usuario. */
	private String mensaje;

	/** Momento en que se generó. */
	private LocalDateTime fecha;

	/** Indica si el usuario ya la leyó. */
	private Boolean leida;

	/** Usuario que recibe la notificación. */
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
	public Notificacion() {
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
	 * Devuelve el usuario que recibe la notificación.
	 *
	 * @return usuario que recibe la notificación
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que recibe la notificación.
	 *
	 * @param idUsuario usuario que recibe la notificación
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}
}
