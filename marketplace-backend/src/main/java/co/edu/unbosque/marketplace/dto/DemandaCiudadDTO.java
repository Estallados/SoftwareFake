package co.edu.unbosque.marketplace.dto;

/**
 * Cantidad de reservas de una ciudad.
 *
 * @see "RF-005, CA-026"
 */
public class DemandaCiudadDTO {
	private String ciudad;
	private Long totalReservas;

	public DemandaCiudadDTO() {
	}

	public DemandaCiudadDTO(String ciudad, Long totalReservas) {
		this.ciudad = ciudad;
		this.totalReservas = totalReservas;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public Long getTotalReservas() {
		return totalReservas;
	}

	public void setTotalReservas(Long totalReservas) {
		this.totalReservas = totalReservas;
	}
}
