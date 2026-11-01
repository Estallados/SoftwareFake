package co.edu.unbosque.marketplace.dto;

import java.util.Map;

/**
 * Total de una entidad y su cantidad por estado (dashboard). {@code porEstado}
 * trae todos los estados, con 0 en los que no tienen filas.
 *
 * @see "RF-005"
 */
public class ConteoPorEstadoDTO {
	private Long total;
	private Map<String, Long> porEstado;

	public ConteoPorEstadoDTO() {
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public Map<String, Long> getPorEstado() {
		return porEstado;
	}

	public void setPorEstado(Map<String, Long> porEstado) {
		this.porEstado = porEstado;
	}
}
