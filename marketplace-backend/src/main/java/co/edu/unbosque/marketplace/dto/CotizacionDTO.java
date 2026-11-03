package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cotización de una estancia. De entrada se usan idAlojamiento, fechas,
 * huespedes y serviciosSeleccionados; el resto lo calcula el service.
 *
 * @see "RF-021, RNF-031, CA-179"
 */
public class CotizacionDTO {
	private Long idCotizacion;
	private Long idAlojamiento;
	private LocalDate fechaEntrada;
	private LocalDate fechaSalida;
	private Integer huespedes;
	private Integer noches;
	private BigDecimal precioNoche;
	private BigDecimal precioBase;
	private BigDecimal descuento;
	private BigDecimal porcentajeLimpieza;
	private BigDecimal valorLimpieza;
	private BigDecimal porcentajeServicio;
	private BigDecimal valorServicio;
	private BigDecimal valorAdicionales;
	private BigDecimal total;
	private List<Long> serviciosSeleccionados;
	private List<ServicioAdicionalDTO> serviciosAdicionales;
	private List<ServicioAdicionalDTO> serviciosDisponibles;
	private LocalDateTime fechaExpiracion;
	private Long idReserva;
	private Long idUsuario;
	private LocalDateTime fechaCreacion;
	private String desglose;

	public CotizacionDTO() {
	}

	public Long getIdCotizacion() {
		return idCotizacion;
	}

	public void setIdCotizacion(Long idCotizacion) {
		this.idCotizacion = idCotizacion;
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

	public Integer getHuespedes() {
		return huespedes;
	}

	public void setHuespedes(Integer huespedes) {
		this.huespedes = huespedes;
	}

	public Integer getNoches() {
		return noches;
	}

	public void setNoches(Integer noches) {
		this.noches = noches;
	}

	public BigDecimal getPrecioNoche() {
		return precioNoche;
	}

	public void setPrecioNoche(BigDecimal precioNoche) {
		this.precioNoche = precioNoche;
	}

	public BigDecimal getPrecioBase() {
		return precioBase;
	}

	public void setPrecioBase(BigDecimal precioBase) {
		this.precioBase = precioBase;
	}

	public BigDecimal getDescuento() {
		return descuento;
	}

	public void setDescuento(BigDecimal descuento) {
		this.descuento = descuento;
	}

	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	public BigDecimal getValorLimpieza() {
		return valorLimpieza;
	}

	public void setValorLimpieza(BigDecimal valorLimpieza) {
		this.valorLimpieza = valorLimpieza;
	}

	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	public BigDecimal getValorServicio() {
		return valorServicio;
	}

	public void setValorServicio(BigDecimal valorServicio) {
		this.valorServicio = valorServicio;
	}

	public BigDecimal getValorAdicionales() {
		return valorAdicionales;
	}

	public void setValorAdicionales(BigDecimal valorAdicionales) {
		this.valorAdicionales = valorAdicionales;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public List<Long> getServiciosSeleccionados() {
		return serviciosSeleccionados;
	}

	public void setServiciosSeleccionados(List<Long> serviciosSeleccionados) {
		this.serviciosSeleccionados = serviciosSeleccionados;
	}

	public List<ServicioAdicionalDTO> getServiciosAdicionales() {
		return serviciosAdicionales;
	}

	public void setServiciosAdicionales(List<ServicioAdicionalDTO> serviciosAdicionales) {
		this.serviciosAdicionales = serviciosAdicionales;
	}

	public List<ServicioAdicionalDTO> getServiciosDisponibles() {
		return serviciosDisponibles;
	}

	public void setServiciosDisponibles(List<ServicioAdicionalDTO> serviciosDisponibles) {
		this.serviciosDisponibles = serviciosDisponibles;
	}

	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	public Long getIdReserva() {
		return idReserva;
	}

	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public String getDesglose() {
		return desglose;
	}

	public void setDesglose(String desglose) {
		this.desglose = desglose;
	}
}
