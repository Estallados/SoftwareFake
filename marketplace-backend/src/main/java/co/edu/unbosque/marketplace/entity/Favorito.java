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
 * Alojamiento que un usuario guardó en sus favoritos para consultarlo después.
 * Un alojamiento aparece una sola vez en los favoritos de cada usuario.
 */
@Entity
@Table(name = "favorito")
public class Favorito {

	/** Identificador del favorito. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idFavorito;

	/** Momento en que se agregó a favoritos. */
	private LocalDateTime fechaAgregado;

	/** Usuario que guardó el favorito. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Alojamiento guardado. */
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
	public Favorito() {
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
	 * Devuelve el usuario que guardó el favorito.
	 *
	 * @return usuario que guardó el favorito
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que guardó el favorito.
	 *
	 * @param idUsuario usuario que guardó el favorito
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
}
