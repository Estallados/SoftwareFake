package co.edu.unbosque.marketplace.dto;

/**
 * Datos para confirmar el correo: el correo de la cuenta y el código de 6
 * dígitos que llegó en el correo de bienvenida.
 *
 * @see "RF-001"
 */
public class VerificacionCorreoDTO {
	private String correo;
	private String codigo;

	public VerificacionCorreoDTO() {
	}

	public String getCorreo() {
		return correo;
	}

	public void setCorreo(String correo) {
		this.correo = correo;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}
}
