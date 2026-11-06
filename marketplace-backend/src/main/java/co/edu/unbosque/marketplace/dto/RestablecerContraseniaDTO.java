package co.edu.unbosque.marketplace.dto;

/**
 * Datos para restablecer la contraseña con un token de recuperación.
 */
public class RestablecerContraseniaDTO {

	/** Token que le llegó al usuario por correo. */
	private String token;

	/** Contraseña nueva; debe cumplir las reglas de contraseña. */
	private String contrasenia;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public RestablecerContraseniaDTO() {
	}

	/**
	 * Devuelve el token que le llegó al usuario por correo.
	 *
	 * @return token que le llegó al usuario por correo
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Asigna el token que le llegó al usuario por correo.
	 *
	 * @param token token que le llegó al usuario por correo
	 */
	public void setToken(String token) {
		this.token = token;
	}

	/**
	 * Devuelve la contraseña nueva; debe cumplir las reglas de contraseña.
	 *
	 * @return contraseña nueva; debe cumplir las reglas de contraseña
	 */
	public String getContrasenia() {
		return contrasenia;
	}

	/**
	 * Asigna la contraseña nueva; debe cumplir las reglas de contraseña.
	 *
	 * @param contrasenia contraseña nueva; debe cumplir las reglas de contraseña
	 */
	public void setContrasenia(String contrasenia) {
		this.contrasenia = contrasenia;
	}
}
