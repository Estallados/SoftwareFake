package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

/**
 * Código de verificación de correo. Se envía al registrarse y se conserva solo
 * su hash, en memoria y sin pasar por la base de datos; el código en claro
 * únicamente viaja en el correo al usuario. Vence, sirve una sola vez y admite
 * pocos intentos.
 *
 * @see "RF-001"
 */
public class CodigoVerificacion {
	private Long idUsuario;
	private String codigoHash;
	private LocalDateTime fechaCreacion;
	private LocalDateTime fechaExpiracion;
	/** Intentos fallidos con este código; al llegar al máximo deja de servir. */
	private Integer intentos;
	private Boolean usado;

	public CodigoVerificacion() {
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getCodigoHash() {
		return codigoHash;
	}

	public void setCodigoHash(String codigoHash) {
		this.codigoHash = codigoHash;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	public Integer getIntentos() {
		return intentos;
	}

	public void setIntentos(Integer intentos) {
		this.intentos = intentos;
	}

	public Boolean getUsado() {
		return usado;
	}

	public void setUsado(Boolean usado) {
		this.usado = usado;
	}
}
