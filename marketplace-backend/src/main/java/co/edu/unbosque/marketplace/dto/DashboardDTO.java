package co.edu.unbosque.marketplace.dto;

import java.util.List;

/**
 * Indicadores del dashboard administrativo, ya calculados con consultas
 * agregadas (sin listas de entidades).
 *
 * @see "RF-005"
 */
public class DashboardDTO {
	private Long totalUsuarios;
	private ConteoPorEstadoDTO alojamientos;
	private ConteoReservasDTO reservas;
	private List<DemandaCiudadDTO> ciudadesTop;

	public DashboardDTO() {
	}

	public Long getTotalUsuarios() {
		return totalUsuarios;
	}

	public void setTotalUsuarios(Long totalUsuarios) {
		this.totalUsuarios = totalUsuarios;
	}

	public ConteoPorEstadoDTO getAlojamientos() {
		return alojamientos;
	}

	public void setAlojamientos(ConteoPorEstadoDTO alojamientos) {
		this.alojamientos = alojamientos;
	}

	public ConteoReservasDTO getReservas() {
		return reservas;
	}

	public void setReservas(ConteoReservasDTO reservas) {
		this.reservas = reservas;
	}

	public List<DemandaCiudadDTO> getCiudadesTop() {
		return ciudadesTop;
	}

	public void setCiudadesTop(List<DemandaCiudadDTO> ciudadesTop) {
		this.ciudadesTop = ciudadesTop;
	}
}
