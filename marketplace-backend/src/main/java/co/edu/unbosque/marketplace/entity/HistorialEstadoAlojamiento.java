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
 * Cambio de estado de un alojamiento (por ejemplo, de PENDIENTE_REVISION a
 * ACTIVA), con quién lo hizo, cuándo y por qué. Es de solo inserción
 * ({@code @Immutable}): los registros anteriores nunca se modifican.
 */
@Entity
@Immutable
@Table(name = "historialEstadoAlojamiento")
public class HistorialEstadoAlojamiento {

	/** Identificador del registro. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idHistorial;

	/** Alojamiento que cambió de estado. */
	@Column(name = "id_alojamiento", updatable = false)
	private Long idAlojamiento;

	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;

	/** Estado antes del cambio; {@code null} al crear el alojamiento. */
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private EstadoAlojamiento estadoAnterior;

	/** Estado después del cambio. */
	@Enumerated(EnumType.STRING)
	@Column(updatable = false)
	private EstadoAlojamiento estadoNuevo;

	/** Motivo del cambio (obligatorio al rechazar o bloquear). */
	@Column(updatable = false)
	private String motivo;

	/** Usuario que hizo el cambio (el anfitrión o un administrador). */
	@Column(name = "id_usuario", updatable = false)
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Momento del cambio. */
	@Column(updatable = false)
	private LocalDateTime fecha;

	/** Constructor vacío, requerido por JPA. */
	public HistorialEstadoAlojamiento() {
	}

	/**
	 * Devuelve el identificador del registro.
	 *
	 * @return identificador del registro
	 */
	public Long getIdHistorial() {
		return idHistorial;
	}

	/**
	 * Asigna el identificador del registro.
	 *
	 * @param idHistorial identificador del registro
	 */
	public void setIdHistorial(Long idHistorial) {
		this.idHistorial = idHistorial;
	}

	/**
	 * Devuelve el alojamiento que cambió de estado.
	 *
	 * @return alojamiento que cambió de estado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento que cambió de estado.
	 *
	 * @param idAlojamiento alojamiento que cambió de estado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el estado antes del cambio; {@code null} al crear el alojamiento.
	 *
	 * @return estado antes del cambio; {@code null} al crear el alojamiento
	 */
	public EstadoAlojamiento getEstadoAnterior() {
		return estadoAnterior;
	}

	/**
	 * Asigna el estado antes del cambio; {@code null} al crear el alojamiento.
	 *
	 * @param estadoAnterior estado antes del cambio; {@code null} al crear el alojamiento
	 */
	public void setEstadoAnterior(EstadoAlojamiento estadoAnterior) {
		this.estadoAnterior = estadoAnterior;
	}

	/**
	 * Devuelve el estado después del cambio.
	 *
	 * @return estado después del cambio
	 */
	public EstadoAlojamiento getEstadoNuevo() {
		return estadoNuevo;
	}

	/**
	 * Asigna el estado después del cambio.
	 *
	 * @param estadoNuevo estado después del cambio
	 */
	public void setEstadoNuevo(EstadoAlojamiento estadoNuevo) {
		this.estadoNuevo = estadoNuevo;
	}

	/**
	 * Devuelve el motivo del cambio (obligatorio al rechazar o bloquear).
	 *
	 * @return motivo del cambio (obligatorio al rechazar o bloquear)
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna el motivo del cambio (obligatorio al rechazar o bloquear).
	 *
	 * @param motivo motivo del cambio (obligatorio al rechazar o bloquear)
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	/**
	 * Devuelve el usuario que hizo el cambio (el anfitrión o un administrador).
	 *
	 * @return usuario que hizo el cambio (el anfitrión o un administrador)
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que hizo el cambio (el anfitrión o un administrador).
	 *
	 * @param idUsuario usuario que hizo el cambio (el anfitrión o un administrador)
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el momento del cambio.
	 *
	 * @return momento del cambio
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del cambio.
	 *
	 * @param fecha momento del cambio
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
