package co.edu.unbosque.marketplace.dto;

/**
 * Datos para confirmar el correo: el correo de la cuenta y el código de 6
 * dígitos que llegó en el correo de bienvenida.
 */
public class VerificacionCorreoDTO {

	/** Correo de la cuenta. */
	private String correo;

	/** Código de 6 dígitos. */
	private String codigo;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public VerificacionCorreoDTO() {
	}

	/**
	 * Devuelve el correo de la cuenta.
	 *
	 * @return correo de la cuenta
	 */
	public String getCorreo() {
		return correo;
	}

	/**
	 * Asigna el correo de la cuenta.
	 *
	 * @param correo correo de la cuenta
	 */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/**
	 * Devuelve el código de 6 dígitos.
	 *
	 * @return código de 6 dígitos
	 */
	public String getCodigo() {
		return codigo;
	}

	/**
	 * Asigna el código de 6 dígitos.
	 *
	 * @param codigo código de 6 dígitos
	 */
	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
}
