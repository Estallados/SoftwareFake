package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

/**
 * Token de recuperación de contraseña. Se conserva solo su hash, en memoria y
 * sin pasar por la base de datos; el token en claro únicamente se entrega al
 * usuario (en esta versión, simulado en el log).
 *
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
public class TokenRecuperacion {
	private Long idUsuario;
	private String tokenHash;
	private LocalDateTime fechaExpiracion;
	private Boolean usado;

	public TokenRecuperacion() {
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public void setTokenHash(String tokenHash) {
		this.tokenHash = tokenHash;
	}

	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	public Boolean getUsado() {
		return usado;
	}

	public void setUsado(Boolean usado) {
		this.usado = usado;
	}
}
