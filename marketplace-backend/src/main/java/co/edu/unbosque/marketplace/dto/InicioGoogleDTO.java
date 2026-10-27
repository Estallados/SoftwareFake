package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.entity.Role;

/**
 * Datos para iniciar sesión con Google: el ID token ({@code credential}) que
 * entrega el botón de Google en el front, el rol con el que se crea la cuenta
 * si es la primera vez (HUESPED por defecto, o ANFITRION) y el código del
 * segundo factor si la cuenta lo tiene activo.
 *
 * @see "RF-008"
 */
public class InicioGoogleDTO {
    private String idToken;
    private Role rol;
    private String codigo;

    public InicioGoogleDTO() {
    }

    public String getIdToken() {
        return idToken;
    }
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public Role getRol() {
        return rol;
    }
    public void setRol(Role rol) {
        this.rol = rol;
    }

    public String getCodigo() {
        return codigo;
    }
    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
