package co.edu.unbosque.marketplace.dto;

/**
 * Datos para restablecer la contraseña con un token de recuperación.
 *
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
public class RestablecerContraseniaDTO {
	private String token;
	private String contrasenia;

	public RestablecerContraseniaDTO() {
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getContrasenia() {
		return contrasenia;
	}

	public void setContrasenia(String contrasenia) {
		this.contrasenia = contrasenia;
	}
}
