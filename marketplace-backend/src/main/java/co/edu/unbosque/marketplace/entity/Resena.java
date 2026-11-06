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
 * Reseña que deja un huésped sobre un alojamiento después de su estancia. Hay
 * como máximo una por reserva. Cuando un administrador la oculta no se borra:
 * solo deja de mostrarse.
 */
@Entity
@Table(name = "resena")
public class Resena {

	/** Identificador de la reseña. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idResena;

	/** Calificación de 1 a 5 estrellas. */
	private Integer calificacion;

	/** Comentario del huésped. */
	private String comentario;

	/** Si la reseña está publicada u oculta. */
	@Enumerated(EnumType.STRING)
	private EstadoResena estado;

	/** Momento en que se publicó. */
	private LocalDateTime fecha;

	/** Huésped que escribió la reseña. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Reserva a la que corresponde la reseña. */
	@Column(name = "id_reserva")
	private Long idReserva;

	/**
	 * Clave foránea hacia {@link Reserva}; el valor se asigna con
	 * {@code idReserva}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_reserva", insertable = false, updatable = false)
	private Reserva reserva;

	/** Alojamiento reseñado. */
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
	public Resena() {
	}

	/**
	 * Devuelve el identificador de la reseña.
	 *
	 * @return identificador de la reseña
	 */
	public Long getIdResena() {
		return idResena;
	}

	/**
	 * Asigna el identificador de la reseña.
	 *
	 * @param idResena identificador de la reseña
	 */
	public void setIdResena(Long idResena) {
		this.idResena = idResena;
	}

	/**
	 * Devuelve la calificación de 1 a 5 estrellas.
	 *
	 * @return calificación de 1 a 5 estrellas
	 */
	public Integer getCalificacion() {
		return calificacion;
	}

	/**
	 * Asigna la calificación de 1 a 5 estrellas.
	 *
	 * @param calificacion calificación de 1 a 5 estrellas
	 */
	public void setCalificacion(Integer calificacion) {
		this.calificacion = calificacion;
	}

	/**
	 * Devuelve el comentario del huésped.
	 *
	 * @return comentario del huésped
	 */
	public String getComentario() {
		return comentario;
	}

	/**
	 * Asigna el comentario del huésped.
	 *
	 * @param comentario comentario del huésped
	 */
	public void setComentario(String comentario) {
		this.comentario = comentario;
	}

	/**
	 * Devuelve si la reseña está publicada u oculta.
	 *
	 * @return si la reseña está publicada u oculta
	 */
	public EstadoResena getEstado() {
		return estado;
	}

	/**
	 * Asigna si la reseña está publicada u oculta.
	 *
	 * @param estado si la reseña está publicada u oculta
	 */
	public void setEstado(EstadoResena estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el momento en que se publicó.
	 *
	 * @return momento en que se publicó
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento en que se publicó.
	 *
	 * @param fecha momento en que se publicó
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve el huésped que escribió la reseña.
	 *
	 * @return huésped que escribió la reseña
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el huésped que escribió la reseña.
	 *
	 * @param idUsuario huésped que escribió la reseña
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve la reserva a la que corresponde la reseña.
	 *
	 * @return reserva a la que corresponde la reseña
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva a la que corresponde la reseña.
	 *
	 * @param idReserva reserva a la que corresponde la reseña
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	/**
	 * Devuelve el alojamiento reseñado.
	 *
	 * @return alojamiento reseñado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento reseñado.
	 *
	 * @param idAlojamiento alojamiento reseñado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}
}
