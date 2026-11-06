package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

/**
 * Código de verificación del correo, que se envía al registrarse.
 * <p>
 * No es una tabla: se guarda en memoria y solo su hash. El código en claro
 * únicamente viaja en el correo al usuario. Vence, sirve una sola vez y admite
 * pocos intentos fallidos.
 * </p>
 */
public class CodigoVerificacion {

	/** Usuario dueño del código. */
	private Long idUsuario;

	/** Hash del código de 6 dígitos; el código en claro no se guarda. */
	private String codigoHash;

	/** Momento en que se generó el código. */
	private LocalDateTime fechaCreacion;

	/** Momento en que el código deja de servir. */
	private LocalDateTime fechaExpiracion;

	/** Intentos fallidos con este código; al llegar al máximo deja de servir. */
	private Integer intentos;

	/** Indica si el código ya se usó o fue reemplazado por uno nuevo. */
	private Boolean usado;

	/** Constructor vacío. */
	public CodigoVerificacion() {
	}

	/**
	 * Devuelve el usuario dueño del código.
	 *
	 * @return usuario dueño del código
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario dueño del código.
	 *
	 * @param idUsuario usuario dueño del código
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el hash del código de 6 dígitos; el código en claro no se guarda.
	 *
	 * @return hash del código de 6 dígitos; el código en claro no se guarda
	 */
	public String getCodigoHash() {
		return codigoHash;
	}

	/**
	 * Asigna el hash del código de 6 dígitos; el código en claro no se guarda.
	 *
	 * @param codigoHash hash del código de 6 dígitos; el código en claro no se guarda
	 */
	public void setCodigoHash(String codigoHash) {
		this.codigoHash = codigoHash;
	}

	/**
	 * Devuelve el momento en que se generó el código.
	 *
	 * @return momento en que se generó el código
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que se generó el código.
	 *
	 * @param fechaCreacion momento en que se generó el código
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	/**
	 * Devuelve el momento en que el código deja de servir.
	 *
	 * @return momento en que el código deja de servir
	 */
	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	/**
	 * Asigna el momento en que el código deja de servir.
	 *
	 * @param fechaExpiracion momento en que el código deja de servir
	 */
	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	/**
	 * Devuelve los intentos fallidos con este código; al llegar al máximo deja de servir.
	 *
	 * @return intentos fallidos con este código; al llegar al máximo deja de servir
	 */
	public Integer getIntentos() {
		return intentos;
	}

	/**
	 * Asigna los intentos fallidos con este código; al llegar al máximo deja de servir.
	 *
	 * @param intentos intentos fallidos con este código; al llegar al máximo deja de servir
	 */
	public void setIntentos(Integer intentos) {
		this.intentos = intentos;
	}

	/**
	 * Devuelve {@code true} si el código ya se usó o fue reemplazado por uno nuevo.
	 *
	 * @return {@code true} si el código ya se usó o fue reemplazado por uno nuevo
	 */
	public Boolean getUsado() {
		return usado;
	}

	/**
	 * Asigna {@code true} si el código ya se usó o fue reemplazado por uno nuevo.
	 *
	 * @param usado {@code true} si el código ya se usó o fue reemplazado por uno nuevo
	 */
	public void setUsado(Boolean usado) {
		this.usado = usado;
	}
}
