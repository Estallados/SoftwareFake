package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

/**
 * Devolución estimada si el huésped cancelara hoy su reserva. Se calcula con la
 * misma regla que la cancelación real, pero no cambia nada.
 *
 * @see "RF-002, CA-002, CA-003"
 */
public class SimulacionCancelacionDTO {
	private Long idReserva;
	private String politica;
	/** Días que faltan para la llegada, contados desde hoy. */
	private Long diasAnticipacion;
	private Integer porcentajeReembolso;
	private BigDecimal valorReembolso;
	private BigDecimal totalPagado;

	public SimulacionCancelacionDTO() {
	}

	public Long getIdReserva() {
		return idReserva;
	}

	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	public String getPolitica() {
		return politica;
	}

	public void setPolitica(String politica) {
		this.politica = politica;
	}

	public Long getDiasAnticipacion() {
		return diasAnticipacion;
	}

	public void setDiasAnticipacion(Long diasAnticipacion) {
		this.diasAnticipacion = diasAnticipacion;
	}

	public Integer getPorcentajeReembolso() {
		return porcentajeReembolso;
	}

	public void setPorcentajeReembolso(Integer porcentajeReembolso) {
		this.porcentajeReembolso = porcentajeReembolso;
	}

	public BigDecimal getValorReembolso() {
		return valorReembolso;
	}

	public void setValorReembolso(BigDecimal valorReembolso) {
		this.valorReembolso = valorReembolso;
	}

	public BigDecimal getTotalPagado() {
		return totalPagado;
	}

	public void setTotalPagado(BigDecimal totalPagado) {
		this.totalPagado = totalPagado;
	}
}
