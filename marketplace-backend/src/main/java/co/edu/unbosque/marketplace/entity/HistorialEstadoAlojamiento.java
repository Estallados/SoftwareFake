package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que guarda cada cambio de estado de un alojamiento. Es de solo
 * inserción, así se conservan los eventos anteriores.
 *
 * @see "RF-027, RNF-026, CA-153"
 */
@Entity
@Immutable
@Table(name = "historialEstadoAlojamiento")
public class HistorialEstadoAlojamiento {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idHistorial;
	@Column(name = "id_alojamiento", updatable = false)
	private Long idAlojamiento;
	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private EstadoAlojamiento estadoAnterior;
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private EstadoAlojamiento estadoNuevo;
	@Column(updatable = false)
	private String motivo;
	@Column(name = "id_usuario", updatable = false)
	private Long idUsuario;
	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;
	@Column(updatable = false)
	private LocalDateTime fecha;

	public HistorialEstadoAlojamiento() {
	}

	public Long getIdHistorial() {
		return idHistorial;
	}

	public void setIdHistorial(Long idHistorial) {
		this.idHistorial = idHistorial;
	}

	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	public EstadoAlojamiento getEstadoAnterior() {
		return estadoAnterior;
	}

	public void setEstadoAnterior(EstadoAlojamiento estadoAnterior) {
		this.estadoAnterior = estadoAnterior;
	}

	public EstadoAlojamiento getEstadoNuevo() {
		return estadoNuevo;
	}

	public void setEstadoNuevo(EstadoAlojamiento estadoNuevo) {
		this.estadoNuevo = estadoNuevo;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
