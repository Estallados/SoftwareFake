package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

/**
 * Devolución estimada si el huésped cancelara hoy su reserva. Se calcula con
 * la misma regla que la cancelación real, pero no cambia nada; sirve para que
 * el huésped decida antes de cancelar.
 */
public class SimulacionCancelacionDTO {
	/** Reserva consultada. */
	private Long idReserva;

	/** Política de cancelación del alojamiento. */
	private String politica;
	/** Días que faltan para la llegada, contados desde hoy. */
	private Long diasAnticipacion;

	/** Porcentaje que se devolvería. */
	private Integer porcentajeReembolso;

	/** Valor que se devolvería. */
	private BigDecimal valorReembolso;

	/** Total pagado por la reserva. */
	private BigDecimal totalPagado;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public SimulacionCancelacionDTO() {
	}

	/**
	 * Devuelve la reserva consultada.
	 *
	 * @return reserva consultada
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva consultada.
	 *
	 * @param idReserva reserva consultada
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	/**
	 * Devuelve la política de cancelación del alojamiento.
	 *
	 * @return política de cancelación del alojamiento
	 */
	public String getPolitica() {
		return politica;
	}

	/**
	 * Asigna la política de cancelación del alojamiento.
	 *
	 * @param politica política de cancelación del alojamiento
	 */
	public void setPolitica(String politica) {
		this.politica = politica;
	}

	/**
	 * Devuelve los días que faltan para la llegada, contados desde hoy.
	 *
	 * @return días que faltan para la llegada, contados desde hoy
	 */
	public Long getDiasAnticipacion() {
		return diasAnticipacion;
	}

	/**
	 * Asigna los días que faltan para la llegada, contados desde hoy.
	 *
	 * @param diasAnticipacion días que faltan para la llegada, contados desde hoy
	 */
	public void setDiasAnticipacion(Long diasAnticipacion) {
		this.diasAnticipacion = diasAnticipacion;
	}

	/**
	 * Devuelve el porcentaje que se devolvería.
	 *
	 * @return porcentaje que se devolvería
	 */
	public Integer getPorcentajeReembolso() {
		return porcentajeReembolso;
	}

	/**
	 * Asigna el porcentaje que se devolvería.
	 *
	 * @param porcentajeReembolso porcentaje que se devolvería
	 */
	public void setPorcentajeReembolso(Integer porcentajeReembolso) {
		this.porcentajeReembolso = porcentajeReembolso;
	}

	/**
	 * Devuelve el valor que se devolvería.
	 *
	 * @return valor que se devolvería
	 */
	public BigDecimal getValorReembolso() {
		return valorReembolso;
	}

	/**
	 * Asigna el valor que se devolvería.
	 *
	 * @param valorReembolso valor que se devolvería
	 */
	public void setValorReembolso(BigDecimal valorReembolso) {
		this.valorReembolso = valorReembolso;
	}

	/**
	 * Devuelve el total pagado por la reserva.
	 *
	 * @return total pagado por la reserva
	 */
	public BigDecimal getTotalPagado() {
		return totalPagado;
	}

	/**
	 * Asigna el total pagado por la reserva.
	 *
	 * @param totalPagado total pagado por la reserva
	 */
	public void setTotalPagado(BigDecimal totalPagado) {
		this.totalPagado = totalPagado;
	}
}
