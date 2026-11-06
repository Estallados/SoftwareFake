package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

/**
 * Token para recuperar la contraseña.
 * <p>
 * No es una tabla: se guarda en memoria y solo su hash. El token en claro se le
 * envía al usuario por correo y nunca queda almacenado. Vence a los 30 minutos
 * y sirve una sola vez.
 * </p>
 */
public class TokenRecuperacion {

	/** Usuario dueño del token. */
	private Long idUsuario;

	/** Hash del token; el token en claro no se guarda. */
	private String tokenHash;

	/** Momento en que el token deja de servir. */
	private LocalDateTime fechaExpiracion;

	/** Indica si el token ya se usó o fue reemplazado por uno nuevo. */
	private Boolean usado;

	/** Constructor vacío. */
	public TokenRecuperacion() {
	}

	/**
	 * Devuelve el usuario dueño del token.
	 *
	 * @return usuario dueño del token
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario dueño del token.
	 *
	 * @param idUsuario usuario dueño del token
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el hash del token; el token en claro no se guarda.
	 *
	 * @return hash del token; el token en claro no se guarda
	 */
	public String getTokenHash() {
		return tokenHash;
	}

	/**
	 * Asigna el hash del token; el token en claro no se guarda.
	 *
	 * @param tokenHash hash del token; el token en claro no se guarda
	 */
	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	/**
	 * Devuelve el momento en que el token deja de servir.
	 *
	 * @return momento en que el token deja de servir
	 */
	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	/**
	 * Asigna el momento en que el token deja de servir.
	 *
	 * @param fechaExpiracion momento en que el token deja de servir
	 */
	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	/**
	 * Devuelve {@code true} si el token ya se usó o fue reemplazado por uno nuevo.
	 *
	 * @return {@code true} si el token ya se usó o fue reemplazado por uno nuevo
	 */
	public Boolean getUsado() {
		return usado;
	}

	/**
	 * Asigna {@code true} si el token ya se usó o fue reemplazado por uno nuevo.
	 *
	 * @param usado {@code true} si el token ya se usó o fue reemplazado por uno nuevo
	 */
	public void setUsado(Boolean usado) {
		this.usado = usado;
	}
}
