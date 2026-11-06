package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Conversación entre un huésped ({@code idUsuario}) y el anfitrión
 * ({@code idAnfitrion}) sobre un alojamiento. Los mensajes se guardan en
 * {@link Mensaje}.
 */
@Entity
@Table(name = "conversacion")
public class Conversacion {

	/** Identificador de la conversación. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idConversacion;

	/** Momento en que se inició. */
	private LocalDateTime fechaCreacion;

	/** Estado de la conversación. */
	@Enumerated(EnumType.STRING)
	private EstadoConversacion estado;

	/** Huésped que inició la conversación. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario huesped;

	/** Anfitrión dueño del alojamiento. */
	@Column(name = "id_anfitrion")
	private Long idAnfitrion;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idAnfitrion}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_anfitrion", insertable = false, updatable = false)
	private Usuario anfitrion;

	/** Alojamiento sobre el que se conversa. */
	@Column(name = "id_alojamiento")
	private Long idAlojamiento;

	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;

	/** Constructor vacío, requerido por JPA. */
	public Conversacion() {
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
	 * Devuelve el huésped que inició la conversación.
	 *
	 * @return huésped que inició la conversación
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el huésped que inició la conversación.
	 *
	 * @param idUsuario huésped que inició la conversación
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el anfitrión dueño del alojamiento.
	 *
	 * @return anfitrión dueño del alojamiento
	 */
	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	/**
	 * Asigna el anfitrión dueño del alojamiento.
	 *
	 * @param idAnfitrion anfitrión dueño del alojamiento
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
}
