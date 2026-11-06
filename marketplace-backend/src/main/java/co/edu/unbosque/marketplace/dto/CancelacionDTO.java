package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoCancelacion;
import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Registro de la cancelación de una reserva: quién y por qué la canceló, qué
 * política se aplicó y cuánto se devolvió.
 */
public class CancelacionDTO {
	/** Identificador de la cancelación. */
	private Long idCancelacion;

	/** Motivo de la cancelación. */
	private String motivo;

	/** Política aplicada. */
	private String politica;

	/** Porcentaje devuelto. */
	private Integer porcentajeReembolso;

	/** Valor devuelto. */
	private BigDecimal valorReembolso;

	/** Estado de la cancelación. */
	private EstadoCancelacion estado;

	/** Estado en que quedó la reserva. */
	private EstadoReserva estadoFinalReserva;

	/** Momento de la cancelación. */
	private LocalDateTime fecha;

	/** Usuario que canceló. */
	private Long idUsuario;

	/** Reserva cancelada. */
	private Long idReserva;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public CancelacionDTO() {
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
	 * Devuelve el motivo de la cancelación.
	 *
	 * @return motivo de la cancelación
	 */
	public String getMotivo() {
		return motivo;
	}

	/**
	 * Asigna el motivo de la cancelación.
	 *
	 * @param motivo motivo de la cancelación
	 */
	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}

	/**
	 * Devuelve la política aplicada.
	 *
	 * @return política aplicada
	 */
	public String getPolitica() {
		return politica;
	}

	/**
	 * Asigna la política aplicada.
	 *
	 * @param politica política aplicada
	 */
	public void setPolitica(String politica) {
		this.politica = politica;
	}

	/**
	 * Devuelve el porcentaje devuelto.
	 *
	 * @return porcentaje devuelto
	 */
	public Integer getPorcentajeReembolso() {
		return porcentajeReembolso;
	}

	/**
	 * Asigna el porcentaje devuelto.
	 *
	 * @param porcentajeReembolso porcentaje devuelto
	 */
	public void setPorcentajeReembolso(Integer porcentajeReembolso) {
		this.porcentajeReembolso = porcentajeReembolso;
	}

	/**
	 * Devuelve el valor devuelto.
	 *
	 * @return valor devuelto
	 */
	public BigDecimal getValorReembolso() {
		return valorReembolso;
	}

	/**
	 * Asigna el valor devuelto.
	 *
	 * @param valorReembolso valor devuelto
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
	 * Devuelve el estado en que quedó la reserva.
	 *
	 * @return estado en que quedó la reserva
	 */
	public EstadoReserva getEstadoFinalReserva() {
		return estadoFinalReserva;
	}

	/**
	 * Asigna el estado en que quedó la reserva.
	 *
	 * @param estadoFinalReserva estado en que quedó la reserva
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
