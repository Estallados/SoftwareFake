package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

import co.edu.unbosque.marketplace.entity.EstadoUsuario;
import co.edu.unbosque.marketplace.entity.Role;

public class UsuarioDTO {
	private Long idUsuario;
	private String nombre;
	private String correo;
	private String telefono;
	private Role rol;
	private EstadoUsuario estado;
	private String contrasenia;
	private String motivoRestriccion;
	private Boolean totpHabilitado;
	private LocalDateTime fechaRegistro;
	private Boolean correoVerificado;

	public UsuarioDTO() {
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

	public String getMotivoRestriccion() {
		return motivoRestriccion;
	}

	public void setMotivoRestriccion(String motivoRestriccion) {
		this.motivoRestriccion = motivoRestriccion;
	}

	public Boolean getTotpHabilitado() {
		return totpHabilitado;
	}

	public void setTotpHabilitado(Boolean totpHabilitado) {
		this.totpHabilitado = totpHabilitado;
	}

	public LocalDateTime getFechaRegistro() {
		return fechaRegistro;
	}

	public void setFechaRegistro(LocalDateTime fechaRegistro) {
		this.fechaRegistro = fechaRegistro;
	}

	public Boolean getCorreoVerificado() {
		return correoVerificado;
	}

	public void setCorreoVerificado(Boolean correoVerificado) {
		this.correoVerificado = correoVerificado;
	}
}
