package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
 * Cancelación de una reserva: quién la canceló, cuándo, por qué, qué política
 * se aplicó, cuánto se devolvió y en qué estado quedó la reserva.
 */
@Entity
@Table(name = "cancelacion")
public class Cancelacion {

	/** Identificador de la cancelación. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idCancelacion;

	/** Motivo que escribió quien canceló. */
	private String motivo;

	/** Nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA). */
	private String politica;

	/** Porcentaje del total que se devolvió (0, 50 o 100). */
	private Integer porcentajeReembolso;

	/** Valor devuelto, en pesos colombianos. */
	private BigDecimal valorReembolso;

	/** Estado de la cancelación. */
	@Enumerated(EnumType.STRING)
	private EstadoCancelacion estado;

	/** Estado en que quedó la reserva después de cancelarla. */
	@Enumerated(EnumType.STRING)
	private EstadoReserva estadoFinalReserva;

	/** Momento de la cancelación. */
	private LocalDateTime fecha;

	/** Usuario que canceló. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Reserva cancelada. */
	@Column(name = "id_reserva")
	private Long idReserva;

	/**
	 * Clave foránea hacia {@link Reserva}; el valor se asigna con
	 * {@code idReserva}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_reserva", insertable = false, updatable = false)
	private Reserva reserva;

	/** Constructor vacío, requerido por JPA. */
	public Cancelacion() {
	}

	/**
	 * Devuelve el identificador de la cancelación.
	 *
	 * @return identificador de la cancelación
	 */
	public Long getIdCancelacion() {
		return idCancelacion;
	}

	/**
	 * Asigna el identificador de la cancelación.
	 *
	 * @param idCancelacion identificador de la cancelación
	 */
	public void setIdCancelacion(Long idCancelacion) {
		this.idCancelacion = idCancelacion;
	}

	/**
	 * Devuelve el motivo que escribió quien canceló.
	 *
	 * @return motivo que escribió quien canceló
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna el motivo que escribió quien canceló.
	 *
	 * @param motivo motivo que escribió quien canceló
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	/**
	 * Devuelve el nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA).
	 *
	 * @return nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA)
	 */
	public String getPolitica() {
		return politica;
	}

	/**
	 * Asigna el nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA).
	 *
	 * @param politica nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA)
	 */
	public void setPolitica(String politica) {
		this.politica = politica;
	}

	/**
	 * Devuelve el porcentaje del total que se devolvió (0, 50 o 100).
	 *
	 * @return porcentaje del total que se devolvió (0, 50 o 100)
	 */
	public Integer getPorcentajeReembolso() {
		return porcentajeReembolso;
	}

	/**
	 * Asigna el porcentaje del total que se devolvió (0, 50 o 100).
	 *
	 * @param porcentajeReembolso porcentaje del total que se devolvió (0, 50 o 100)
	 */
	public void setPorcentajeReembolso(Integer porcentajeReembolso) {
		this.porcentajeReembolso = porcentajeReembolso;
	}

	/**
	 * Devuelve el valor devuelto, en pesos colombianos.
	 *
	 * @return valor devuelto, en pesos colombianos
	 */
	public BigDecimal getValorReembolso() {
		return valorReembolso;
	}

	/**
	 * Asigna el valor devuelto, en pesos colombianos.
	 *
	 * @param valorReembolso valor devuelto, en pesos colombianos
	 */
	public void setValorReembolso(BigDecimal valorReembolso) {
		this.valorReembolso = valorReembolso;
	}

	/**
	 * Devuelve el estado de la cancelación.
	 *
	 * @return estado de la cancelación
	 */
	public EstadoCancelacion getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado de la cancelación.
	 *
	 * @param estado estado de la cancelación
	 */
	public void setEstado(EstadoCancelacion estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el estado en que quedó la reserva después de cancelarla.
	 *
	 * @return estado en que quedó la reserva después de cancelarla
	 */
	public EstadoReserva getEstadoFinalReserva() {
		return estadoFinalReserva;
	}

	/**
	 * Asigna el estado en que quedó la reserva después de cancelarla.
	 *
	 * @param estadoFinalReserva estado en que quedó la reserva después de cancelarla
	 */
	public void setEstadoFinalReserva(EstadoReserva estadoFinalReserva) {
		this.estadoFinalReserva = estadoFinalReserva;
	}

	/**
	 * Devuelve el momento de la cancelación.
	 *
	 * @return momento de la cancelación
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento de la cancelación.
	 *
	 * @param fecha momento de la cancelación
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve el usuario que canceló.
	 *
	 * @return usuario que canceló
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el usuario que canceló.
	 *
	 * @param idUsuario usuario que canceló
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve la reserva cancelada.
	 *
	 * @return reserva cancelada
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva cancelada.
	 *
	 * @param idReserva reserva cancelada
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}
}
