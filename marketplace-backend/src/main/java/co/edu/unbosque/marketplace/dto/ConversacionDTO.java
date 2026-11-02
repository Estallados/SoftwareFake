package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoConversacion;

/**
 * Conversación entre huésped (idUsuario) y anfitrión, con sus nombres. De entrada se usa
 * idAlojamiento y opcionalmente un primer mensaje.
 *
 * @see "RF-029"
 */
public class ConversacionDTO {
	private Long idConversacion;
	private LocalDateTime fechaCreacion;
	private EstadoConversacion estado;
	private Long idUsuario;
	private Long idAnfitrion;
	private Long idAlojamiento;
	private String mensaje;
	private String nombreUsuario;
	private String nombreAnfitrion;

	public ConversacionDTO() {
	}

	public Long getIdConversacion() {
		return idConversacion;
	}

	public void setIdConversacion(Long idConversacion) {
		this.idConversacion = idConversacion;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public EstadoConversacion getEstado() {
		return estado;
	}

	public void setEstado(EstadoConversacion estado) {
		this.estado = estado;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	public String getMensaje() {
		return mensaje;
	}

	public void setMensaje(String mensaje) {
		this.mensaje = mensaje;
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	public String getNombreAnfitrion() {
		return nombreAnfitrion;
	}

	public void setNombreAnfitrion(String nombreAnfitrion) {
		this.nombreAnfitrion = nombreAnfitrion;
	}
}
