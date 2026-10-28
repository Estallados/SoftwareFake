package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Resultado de consultar si un alojamiento está libre en unas fechas.
 *
 * @see "RF-026, CA-119, CA-120"
 */
public class ConsultaDisponibilidadDTO {
	private Long idAlojamiento;
	private LocalDate fechaEntrada;
	private LocalDate fechaSalida;
	private Boolean disponible;
	private String motivo;

	public ConsultaDisponibilidadDTO() {
	}

	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	public LocalDate getFechaEntrada() {
		return fechaEntrada;
	}

	public void setFechaEntrada(LocalDate fechaEntrada) {
		this.fechaEntrada = fechaEntrada;
	}

	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}

	public Boolean getDisponible() {
		return disponible;
	}

	public void setDisponible(Boolean disponible) {
		this.disponible = disponible;
	}

	public String getMotivo() {
		return motivo;
	}

	public void setMotivo(String motivo) {
		this.motivo = motivo;
	}
}
