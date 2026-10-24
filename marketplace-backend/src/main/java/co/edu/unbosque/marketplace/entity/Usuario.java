package co.edu.unbosque.marketplace.entity;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import co.edu.unbosque.marketplace.util.AESUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;


/** Entidad que representa usuario en el marketplace. */
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails{
	
    private static final long serialVersionUID = 1L;
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUsuario;
    private String nombre;
    private String correo;
    /** HMAC del correo normalizado: permite buscar por correo aunque esté cifrado. */
    @Column(unique = true)
    private String correoHash;
    private String telefono;
    @Enumerated(EnumType.STRING)
    private Role rol;
    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;
    private String contrasenia;

    public Usuario() {
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return estado != EstadoUsuario.RESTRINGIDO;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCorreo() {
        return correo;
    }
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getCorreoHash() {
        return correoHash;
    }
    public void setCorreoHash(String correoHash) {
        this.correoHash = correoHash;
    }

    public String getTelefono() {
        return telefono;
    }
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Role getRol() {
        return rol;
    }
    public void setRol(Role rol) {
        this.rol = rol;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }
    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

	public String getContrasenia() {
		return contrasenia;
	}

	public void setContrasenia(String contrasenia) {
		this.contrasenia = contrasenia;
	}

    @Override
    public boolean isEnabled() {
        return estado == EstadoUsuario.ACTIVO;
    }
    
    @Override
    public String getPassword() {
        return contrasenia;
    }

    @Override
    public String getUsername() {
        return AESUtil.decrypt(correo);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
            new SimpleGrantedAuthority("ROLE_" + rol.name())
        );
    }

    
}
