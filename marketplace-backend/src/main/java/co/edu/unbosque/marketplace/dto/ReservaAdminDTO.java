package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Fila resumida del listado de reservas del administrador: sin desglose, pago
 * ni cancelación. Se arma directamente en la consulta (una sola por página).
 *
 * @see "RF-016"
 */
public class ReservaAdminDTO {
	private Long idReserva;
	private String nombreHuesped;
	private String tituloAlojamiento;
	private String ciudad;
	private String nombreAnfitrion;
	private LocalDate fechaEntrada;
	private LocalDate fechaSalida;
	private Integer noches;
	private BigDecimal total;
	private EstadoReserva estado;

	public ReservaAdminDTO() {
	}

	@SuppressWarnings("java:S107") // Constructor de la proyección JPQL: un argumento por columna.
	public ReservaAdminDTO(Long idReserva, String nombreHuesped, String tituloAlojamiento, String ciudad,
			String nombreAnfitrion, LocalDate fechaEntrada, LocalDate fechaSalida, Integer noches, BigDecimal total,
			EstadoReserva estado) {
		this.idReserva = idReserva;
		this.nombreHuesped = nombreHuesped;
		this.tituloAlojamiento = tituloAlojamiento;
		this.ciudad = ciudad;
		this.nombreAnfitrion = nombreAnfitrion;
		this.fechaEntrada = fechaEntrada;
		this.fechaSalida = fechaSalida;
		this.noches = noches;
		this.total = total;
		this.estado = estado;
	}

	public Long getIdReserva() {
		return idReserva;
	}

	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	public String getNombreHuesped() {
		return nombreHuesped;
	}

	public void setNombreHuesped(String nombreHuesped) {
		this.nombreHuesped = nombreHuesped;
	}

	public String getTituloAlojamiento() {
		return tituloAlojamiento;
	}

	public void setTituloAlojamiento(String tituloAlojamiento) {
		this.tituloAlojamiento = tituloAlojamiento;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getNombreAnfitrion() {
		return nombreAnfitrion;
	}

	public void setNombreAnfitrion(String nombreAnfitrion) {
		this.nombreAnfitrion = nombreAnfitrion;
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

	public Integer getNoches() {
		return noches;
	}

	public void setNoches(Integer noches) {
		this.noches = noches;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public EstadoReserva getEstado() {
		return estado;
	}

	public void setEstado(EstadoReserva estado) {
		this.estado = estado;
	}
}
