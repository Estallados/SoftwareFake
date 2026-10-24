package co.edu.unbosque.marketplace.dto;

/**
 * Credenciales de inicio de sesión; codigo es el segundo factor (TOTP) cuando la cuenta lo tiene activo.
 *
 * @see "RF-008, RNF-033"
 */
public class LoginDTO {
    private String correo;
    private String contrasenia;
    private String codigo;

    public LoginDTO() {
    }

    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasenia() {
        return contrasenia;
    }
    public void setContrasenia(String contrasenia) {
        this.contrasenia = contrasenia;
    }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
