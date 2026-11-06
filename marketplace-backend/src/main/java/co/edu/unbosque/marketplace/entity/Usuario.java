package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import co.edu.unbosque.marketplace.util.AESUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

/**
 * Usuario del marketplace: huésped, anfitrión o administrador.
 * <p>
 * Implementa {@link UserDetails} para que Spring Security lo use directamente
 * en la autenticación. El correo se guarda cifrado con AES y, para poder
 * buscarlo, también se guarda su hash ({@code correoHash}). La contraseña se
 * guarda con BCrypt.
 * </p>
 */
@Entity
@Table(name = "usuario")
public class Usuario implements UserDetails {

	/** Versión de serialización. */
	private static final long serialVersionUID = 1L;

	/** Identificador del usuario. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idUsuario;

	/** Nombre completo. */
	private String nombre;

	/** Correo cifrado con AES; para leerlo se usa {@link #getUsername()}. */
	private String correo;

	/**
	 * HMAC del correo normalizado: permite buscar por correo aunque esté cifrado.
	 */
	private String correoHash;

	/** Teléfono de contacto; es opcional. */
	private String telefono;

	/** Rol del usuario. */
	@Enumerated(EnumType.STRING)
	private Role rol;

	/** Estado de la cuenta. */
	@Enumerated(EnumType.STRING)
	private EstadoUsuario estado;

	/** Contraseña codificada con BCrypt. */
	private String contrasenia;

	/** Motivo que dio el administrador al restringir la cuenta. */
	private String motivoRestriccion;

	/**
	 * Secreto TOTP cifrado con AES; null si nunca se activó el segundo factor.
	 */
	private String secretoTotp;

	/** Indica si el segundo factor está activo y se pide al iniciar sesión. */
	private Boolean totpHabilitado;

	/** Fecha en que se creó la cuenta; null en cuentas anteriores a este campo. */
	private LocalDateTime fechaRegistro;
	/**
	 * Indica si el usuario confirmó su correo con el código de verificación. false
	 * bloquea el inicio de sesión; null (cuentas anteriores a la verificación) se
	 * considera verificado.
	 */
	private Boolean correoVerificado;

	/** Constructor vacío, requerido por JPA. */
	public Usuario() {
	}

	/**
	 * Las cuentas no vencen.
	 *
	 * @return siempre {@code true}
	 */
	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	/**
	 * Una cuenta restringida por un administrador se considera bloqueada: no
	 * puede iniciar sesión y sus tokens dejan de funcionar.
	 *
	 * @return {@code false} si la cuenta está restringida
	 */
	@Override
	public boolean isAccountNonLocked() {
		return estado != EstadoUsuario.RESTRINGIDO;
	}

	/**
	 * Las contraseñas no vencen.
	 *
	 * @return siempre {@code true}
	 */
	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	/**
	 * Devuelve el identificador del usuario.
	 *
	 * @return identificador del usuario
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el identificador del usuario.
	 *
	 * @param idUsuario identificador del usuario
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el nombre completo.
	 *
	 * @return nombre completo
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre completo.
	 *
	 * @param nombre nombre completo
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve el correo cifrado con AES; para leerlo se usa {@link #getUsername()}.
	 *
	 * @return correo cifrado con AES; para leerlo se usa {@link #getUsername()}
	 */
	public String getCorreo() {
		return correo;
	}

	/**
	 * Asigna el correo cifrado con AES; para leerlo se usa {@link #getUsername()}.
	 *
	 * @param correo correo cifrado con AES; para leerlo se usa {@link #getUsername()}
	 */
	public void setCorreo(String correo) {
		this.correo = correo;
	}

	/**
	 * Devuelve HMAC del correo normalizado: permite buscar por correo aunque esté cifrado.
	 *
	 * @return HMAC del correo normalizado: permite buscar por correo aunque esté cifrado
	 */
	public String getCorreoHash() {
		return correoHash;
	}

	/**
	 * Asigna HMAC del correo normalizado: permite buscar por correo aunque esté cifrado.
	 *
	 * @param correoHash HMAC del correo normalizado: permite buscar por correo aunque esté cifrado
	 */
	public void setCorreoHash(String correoHash) {
		this.correoHash = correoHash;
	}

	/**
	 * Devuelve el teléfono de contacto; es opcional.
	 *
	 * @return teléfono de contacto; es opcional
	 */
	public String getTelefono() {
		return telefono;
	}

	/**
	 * Asigna el teléfono de contacto; es opcional.
	 *
	 * @param telefono teléfono de contacto; es opcional
	 */
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	/**
	 * Devuelve el rol del usuario.
	 *
	 * @return rol del usuario
	 */
	public Role getRol() {
		return rol;
	}

	/**
	 * Asigna el rol del usuario.
	 *
	 * @param rol rol del usuario
	 */
	public void setRol(Role rol) {
		this.rol = rol;
	}

	/**
	 * Devuelve el estado de la cuenta.
	 *
	 * @return estado de la cuenta
	 */
	public EstadoUsuario getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado de la cuenta.
	 *
	 * @param estado estado de la cuenta
	 */
	public void setEstado(EstadoUsuario estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve la contraseña codificada con BCrypt.
	 *
	 * @return contraseña codificada con BCrypt
	 */
	public String getContrasenia() {
		return contrasenia;
	}

	/**
	 * Asigna la contraseña codificada con BCrypt.
	 *
	 * @param contrasenia contraseña codificada con BCrypt
	 */
	public void setContrasenia(String contrasenia) {
		this.contrasenia = contrasenia;
	}

	/**
	 * Devuelve el motivo que dio el administrador al restringir la cuenta.
	 *
	 * @return motivo que dio el administrador al restringir la cuenta
	 */
	public String getMotivoRestriccion() {
		return motivoRestriccion;
	}

	/**
	 * Asigna el motivo que dio el administrador al restringir la cuenta.
	 *
	 * @param motivoRestriccion motivo que dio el administrador al restringir la cuenta
	 */
	public void setMotivoRestriccion(String motivoRestriccion) {
		this.motivoRestriccion = motivoRestriccion;
	}

	/**
	 * Devuelve el secreto TOTP cifrado con AES; null si nunca se activó el segundo factor.
	 *
	 * @return secreto TOTP cifrado con AES; null si nunca se activó el segundo factor
	 */
	public String getSecretoTotp() {
		return secretoTotp;
	}

	/**
	 * Asigna el secreto TOTP cifrado con AES; null si nunca se activó el segundo factor.
	 *
	 * @param secretoTotp secreto TOTP cifrado con AES; null si nunca se activó el segundo factor
	 */
	public void setSecretoTotp(String secretoTotp) {
		this.secretoTotp = secretoTotp;
	}

	/**
	 * Devuelve {@code true} si el segundo factor está activo y se pide al iniciar sesión.
	 *
	 * @return {@code true} si el segundo factor está activo y se pide al iniciar sesión
	 */
	public Boolean getTotpHabilitado() {
		return totpHabilitado;
	}

	/**
	 * Asigna {@code true} si el segundo factor está activo y se pide al iniciar sesión.
	 *
	 * @param totpHabilitado {@code true} si el segundo factor está activo y se pide al iniciar sesión
	 */
	public void setTotpHabilitado(Boolean totpHabilitado) {
		this.totpHabilitado = totpHabilitado;
	}

	/**
	 * Devuelve la fecha en que se creó la cuenta; null en cuentas anteriores a este campo.
	 *
	 * @return fecha en que se creó la cuenta; null en cuentas anteriores a este campo
	 */
	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	/**
	 * Asigna la fecha en que se creó la cuenta; null en cuentas anteriores a este campo.
	 *
	 * @param fechaRegistro fecha en que se creó la cuenta; null en cuentas anteriores a este campo
	 */
	public void setFechaRegistro(LocalDateTime fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	/**
	 * Devuelve {@code true} si el usuario confirmó su correo con el código de verificación.
	 *
	 * @return {@code true} si el usuario confirmó su correo con el código de verificación
	 */
	public Boolean getCorreoVerificado() {
		return correoVerificado;
	}

	/**
	 * Asigna {@code true} si el usuario confirmó su correo con el código de verificación.
	 *
	 * @param correoVerificado {@code true} si el usuario confirmó su correo con el código de verificación
	 */
	public void setCorreoVerificado(Boolean correoVerificado) {
		this.correoVerificado = correoVerificado;
	}

	/**
	 * El correo está verificado si se confirmó o si la cuenta es anterior a la
	 * verificación (null).
	 *
	 * @return {@code true} si el correo cuenta como verificado
	 */
	public boolean esCorreoVerificado() {
		return !Boolean.FALSE.equals(correoVerificado);
	}

	/**
	 * Solo las cuentas activas pueden usarse. Una cuenta inactiva (desactivada
	 * por su dueño) o restringida no puede iniciar sesión.
	 *
	 * @return {@code true} si el estado es {@link EstadoUsuario#ACTIVO}
	 */
	@Override
	public boolean isEnabled() {
		return estado == EstadoUsuario.ACTIVO;
	}

	/**
	 * Devuelve la contraseña codificada con BCrypt, para que Spring la compare.
	 *
	 * @return contraseña codificada con BCrypt, para que Spring la compare
	 */
	@Override
	public String getPassword() {
		return contrasenia;
	}

	/**
	 * El nombre de usuario es el correo en claro: se descifra el que está
	 * guardado. Es también el sujeto de los JWT.
	 *
	 * @return correo del usuario
	 * @throws IllegalArgumentException si el correo se cifró con otra clave
	 */
	@Override
	public String getUsername() {
		return AESUtil.decrypt(correo);
	}

	/**
	 * Devuelve el rol como autoridad de Spring, con el prefijo {@code ROLE_}.
	 *
	 * @return el rol como autoridad de Spring, con el prefijo {@code ROLE_}
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + rol.name()));
	}

}
