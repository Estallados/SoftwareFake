package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Alojamiento que el usuario guardó en favoritos, con los datos del
 * alojamiento para mostrar la tarjeta.
 */
public class FavoritoDTO {
	/** Identificador del favorito. */
	private Long idFavorito;

	/** Momento en que se agregó a favoritos. */
	private LocalDateTime fechaAgregado;

	/** Usuario dueño del favorito. */
	private Long idUsuario;

	/** Alojamiento guardado. */
	private Long idAlojamiento;

	/** Datos del alojamiento para mostrar la tarjeta. */
	private AlojamientoDTO alojamiento;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public FavoritoDTO() {
	}

	/**
	 * Devuelve el identificador del favorito.
	 *
	 * @return identificador del favorito
	 */
	public Long getIdFavorito() {
		return idFavorito;
	}

	/**
	 * Asigna el identificador del favorito.
	 *
	 * @param idFavorito identificador del favorito
	 */
	public void setIdFavorito(Long idFavorito) {
		this.idFavorito = idFavorito;
	}

	/**
	 * Devuelve el momento en que se agregó a favoritos.
	 *
	 * @return momento en que se agregó a favoritos
	 */
	public LocalDateTime getFechaAgregado() {
		return fechaAgregado;
	}

	/**
	 * Asigna el momento en que se agregó a favoritos.
	 *
	 * @param fechaAgregado momento en que se agregó a favoritos
	 */
	public void setFechaAgregado(LocalDateTime fechaAgregado) {
		this.fechaAgregado = fechaAgregado;
	}

	/**
	 * Devuelve el usuario dueño del favorito.
	 *
	 * @return usuario dueño del favorito
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario dueño del favorito.
	 *
	 * @param idUsuario usuario dueño del favorito
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el alojamiento guardado.
	 *
	 * @return alojamiento guardado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento guardado.
	 *
	 * @param idAlojamiento alojamiento guardado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve los datos del alojamiento para mostrar la tarjeta.
	 *
	 * @return datos del alojamiento para mostrar la tarjeta
	 */
	public AlojamientoDTO getAlojamiento() {
		return alojamiento;
	}

	/**
	 * Asigna los datos del alojamiento para mostrar la tarjeta.
	 *
	 * @param alojamiento datos del alojamiento para mostrar la tarjeta
	 */
	public void setAlojamiento(AlojamientoDTO alojamiento) {
		this.alojamiento = alojamiento;
	}
}
