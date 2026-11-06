package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoResena;

/**
 * Reseña de una estancia. Para crearla, el huésped manda idReserva,
 * calificacion y comentario; los demás campos los llena el servidor.
 */
public class ResenaDTO {
	/** Identificador de la reseña. */
	private Long idResena;

	/** Calificación de 1 a 5. */
	private Integer calificacion;

	/** Comentario del huésped. */
	private String comentario;

	/** Si está publicada u oculta. */
	private EstadoResena estado;

	/** Momento en que se publicó. */
	private LocalDateTime fecha;

	/** Huésped que la escribió. */
	private Long idUsuario;

	/** Reserva reseñada. */
	private Long idReserva;

	/** Alojamiento reseñado. */
	private Long idAlojamiento;
	/** Nombre del huésped que escribió la reseña. Solo salida. */
	private String nombreUsuario;

	/** Título del alojamiento reseñado. Solo salida. */
	private String tituloAlojamiento;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ResenaDTO() {
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
	 * Devuelve la calificación de 1 a 5.
	 *
	 * @return calificación de 1 a 5
	 */
	public Integer getCalificacion() {
		return calificacion;
	}

	/**
	 * Asigna la calificación de 1 a 5.
	 *
	 * @param calificacion calificación de 1 a 5
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
	 * Devuelve si está publicada u oculta.
	 *
	 * @return si está publicada u oculta
	 */
	public EstadoResena getEstado() {
		return estado;
	}

	/**
	 * Asigna si está publicada u oculta.
	 *
	 * @param estado si está publicada u oculta
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
	 * Devuelve el huésped que la escribió.
	 *
	 * @return huésped que la escribió
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el huésped que la escribió.
	 *
	 * @param idUsuario huésped que la escribió
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve la reserva reseñada.
	 *
	 * @return reserva reseñada
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva reseñada.
	 *
	 * @param idReserva reserva reseñada
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

	/**
	 * Devuelve el nombre del huésped que escribió la reseña.
	 *
	 * @return nombre del huésped que escribió la reseña
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna el nombre del huésped que escribió la reseña.
	 *
	 * @param nombreUsuario nombre del huésped que escribió la reseña
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	/**
	 * Devuelve el título del alojamiento reseñado.
	 *
	 * @return título del alojamiento reseñado
	 */
	public String getTituloAlojamiento() {
		return tituloAlojamiento;
	}

	/**
	 * Asigna el título del alojamiento reseñado.
	 *
	 * @param tituloAlojamiento título del alojamiento reseñado
	 */
	public void setTituloAlojamiento(String tituloAlojamiento) {
		this.tituloAlojamiento = tituloAlojamiento;
	}
}
