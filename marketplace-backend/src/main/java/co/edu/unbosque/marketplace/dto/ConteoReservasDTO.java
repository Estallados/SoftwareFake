package co.edu.unbosque.marketplace.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Conteo de reservas del dashboard. {@code confirmadasFinalizadas} son las
 * CONFIRMADA con fecha de salida anterior a hoy; si es nulo no se envía.
 *
 * @see "RF-005"
 */
public class ConteoReservasDTO extends ConteoPorEstadoDTO {
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Long confirmadasFinalizadas;

	public ConteoReservasDTO() {
	}

	public Long getConfirmadasFinalizadas() {
		return confirmadasFinalizadas;
	}

	public void setConfirmadasFinalizadas(Long confirmadasFinalizadas) {
		this.confirmadasFinalizadas = confirmadasFinalizadas;
	}
}
