package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cotización de una estancia. Para pedirla, el huésped manda idAlojamiento,
 * las fechas, la cantidad de huéspedes y los servicios adicionales que quiere;
 * el resto lo calcula el servidor.
 */
public class CotizacionDTO {
	/** Identificador de la cotización. */
	private Long idCotizacion;

	/** Alojamiento a cotizar. */
	private Long idAlojamiento;

	/** Día de llegada. */
	private LocalDate fechaEntrada;

	/** Día de salida. */
	private LocalDate fechaSalida;

	/** Cantidad de huéspedes. */
	private Integer huespedes;

	/** Cantidad de noches. */
	private Integer noches;

	/** Precio por noche. */
	private BigDecimal precioNoche;

	/** Precio por noche multiplicado por las noches. */
	private BigDecimal precioBase;

	/** Descuento aplicado. */
	private BigDecimal descuento;

	/** Porcentaje de limpieza aplicado. */
	private BigDecimal porcentajeLimpieza;

	/** Valor de la limpieza. */
	private BigDecimal valorLimpieza;

	/** Porcentaje de la tarifa de servicio aplicado. */
	private BigDecimal porcentajeServicio;

	/** Valor de la tarifa de servicio. */
	private BigDecimal valorServicio;

	/** Suma de los servicios adicionales elegidos. */
	private BigDecimal valorAdicionales;

	/** Valor total a pagar. */
	private BigDecimal total;

	/** Ids de los servicios adicionales que eligió el huésped (entrada). */
	private List<Long> serviciosSeleccionados;

	/** Servicios adicionales incluidos, con su costo (salida). */
	private List<ServicioAdicionalDTO> serviciosAdicionales;

	/** Todos los servicios adicionales que ofrece el alojamiento. */
	private List<ServicioAdicionalDTO> serviciosDisponibles;

	/** Momento en que la cotización deja de servir para reservar. */
	private LocalDateTime fechaExpiracion;

	/** Reserva creada con esta cotización; {@code null} si aún no se usa. */
	private Long idReserva;

	/** Huésped que pidió la cotización. */
	private Long idUsuario;

	/** Momento en que se cotizó. */
	private LocalDateTime fechaCreacion;

	/**
	 * Servicios adicionales elegidos en JSON, con nombre y costo congelados.
	 */
	private String desglose;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public CotizacionDTO() {
	}

	/**
	 * Devuelve el identificador de la cotización.
	 *
	 * @return identificador de la cotización
	 */
	public Long getIdCotizacion() {
		return idCotizacion;
	}

	/**
	 * Asigna el identificador de la cotización.
	 *
	 * @param idCotizacion identificador de la cotización
	 */
	public void setIdCotizacion(Long idCotizacion) {
		this.idCotizacion = idCotizacion;
	}

	/**
	 * Devuelve el alojamiento a cotizar.
	 *
	 * @return alojamiento a cotizar
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento a cotizar.
	 *
	 * @param idAlojamiento alojamiento a cotizar
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el día de llegada.
	 *
	 * @return día de llegada
	 */
	public LocalDate getFechaEntrada() {
		return fechaEntrada;
	}

	/**
	 * Asigna el día de llegada.
	 *
	 * @param fechaEntrada día de llegada
	 */
	public void setFechaEntrada(LocalDate fechaEntrada) {
		this.fechaEntrada = fechaEntrada;
	}

	/**
	 * Devuelve el día de salida.
	 *
	 * @return día de salida
	 */
	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	/**
	 * Asigna el día de salida.
	 *
	 * @param fechaSalida día de salida
	 */
	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}

	/**
	 * Devuelve la cantidad de huéspedes.
	 *
	 * @return cantidad de huéspedes
	 */
	public Integer getHuespedes() {
		return huespedes;
	}

	/**
	 * Asigna la cantidad de huéspedes.
	 *
	 * @param huespedes cantidad de huéspedes
	 */
	public void setHuespedes(Integer huespedes) {
		this.huespedes = huespedes;
	}

	/**
	 * Devuelve la cantidad de noches.
	 *
	 * @return cantidad de noches
	 */
	public Integer getNoches() {
		return noches;
	}

	/**
	 * Asigna la cantidad de noches.
	 *
	 * @param noches cantidad de noches
	 */
	public void setNoches(Integer noches) {
		this.noches = noches;
	}

	/**
	 * Devuelve el precio por noche.
	 *
	 * @return precio por noche
	 */
	public BigDecimal getPrecioNoche() {
		return precioNoche;
	}

	/**
	 * Asigna el precio por noche.
	 *
	 * @param precioNoche precio por noche
	 */
	public void setPrecioNoche(BigDecimal precioNoche) {
		this.precioNoche = precioNoche;
	}

	/**
	 * Devuelve el precio por noche multiplicado por las noches.
	 *
	 * @return precio por noche multiplicado por las noches
	 */
	public BigDecimal getPrecioBase() {
		return precioBase;
	}

	/**
	 * Asigna el precio por noche multiplicado por las noches.
	 *
	 * @param precioBase precio por noche multiplicado por las noches
	 */
	public void setPrecioBase(BigDecimal precioBase) {
		this.precioBase = precioBase;
	}

	/**
	 * Devuelve el descuento aplicado.
	 *
	 * @return descuento aplicado
	 */
	public BigDecimal getDescuento() {
		return descuento;
	}

	/**
	 * Asigna el descuento aplicado.
	 *
	 * @param descuento descuento aplicado
	 */
	public void setDescuento(BigDecimal descuento) {
		this.descuento = descuento;
	}

	/**
	 * Devuelve el porcentaje de limpieza aplicado.
	 *
	 * @return porcentaje de limpieza aplicado
	 */
	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	/**
	 * Asigna el porcentaje de limpieza aplicado.
	 *
	 * @param porcentajeLimpieza porcentaje de limpieza aplicado
	 */
	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	/**
	 * Devuelve el valor de la limpieza.
	 *
	 * @return valor de la limpieza
	 */
	public BigDecimal getValorLimpieza() {
		return valorLimpieza;
	}

	/**
	 * Asigna el valor de la limpieza.
	 *
	 * @param valorLimpieza valor de la limpieza
	 */
	public void setValorLimpieza(BigDecimal valorLimpieza) {
		this.valorLimpieza = valorLimpieza;
	}

	/**
	 * Devuelve el porcentaje de la tarifa de servicio aplicado.
	 *
	 * @return porcentaje de la tarifa de servicio aplicado
	 */
	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	/**
	 * Asigna el porcentaje de la tarifa de servicio aplicado.
	 *
	 * @param porcentajeServicio porcentaje de la tarifa de servicio aplicado
	 */
	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	/**
	 * Devuelve el valor de la tarifa de servicio.
	 *
	 * @return valor de la tarifa de servicio
	 */
	public BigDecimal getValorServicio() {
		return valorServicio;
	}

	/**
	 * Asigna el valor de la tarifa de servicio.
	 *
	 * @param valorServicio valor de la tarifa de servicio
	 */
	public void setValorServicio(BigDecimal valorServicio) {
		this.valorServicio = valorServicio;
	}

	/**
	 * Devuelve la suma de los servicios adicionales elegidos.
	 *
	 * @return suma de los servicios adicionales elegidos
	 */
	public BigDecimal getValorAdicionales() {
		return valorAdicionales;
	}

	/**
	 * Asigna la suma de los servicios adicionales elegidos.
	 *
	 * @param valorAdicionales suma de los servicios adicionales elegidos
	 */
	public void setValorAdicionales(BigDecimal valorAdicionales) {
		this.valorAdicionales = valorAdicionales;
	}

	/**
	 * Devuelve el valor total a pagar.
	 *
	 * @return valor total a pagar
	 */
	public BigDecimal getTotal() {
		return total;
	}

	/**
	 * Asigna el valor total a pagar.
	 *
	 * @param total valor total a pagar
	 */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	/**
	 * Devuelve los ids de los servicios adicionales que eligió el huésped (entrada).
	 *
	 * @return ids de los servicios adicionales que eligió el huésped (entrada)
	 */
	public List<Long> getServiciosSeleccionados() {
		return serviciosSeleccionados;
	}

	/**
	 * Asigna los ids de los servicios adicionales que eligió el huésped (entrada).
	 *
	 * @param serviciosSeleccionados ids de los servicios adicionales que eligió el huésped (entrada)
	 */
	public void setServiciosSeleccionados(List<Long> serviciosSeleccionados) {
		this.serviciosSeleccionados = serviciosSeleccionados;
	}

	/**
	 * Devuelve los servicios adicionales incluidos, con su costo (salida).
	 *
	 * @return servicios adicionales incluidos, con su costo (salida)
	 */
	public List<ServicioAdicionalDTO> getServiciosAdicionales() {
		return serviciosAdicionales;
	}

	/**
	 * Asigna los servicios adicionales incluidos, con su costo (salida).
	 *
	 * @param serviciosAdicionales servicios adicionales incluidos, con su costo (salida)
	 */
	public void setServiciosAdicionales(List<ServicioAdicionalDTO> serviciosAdicionales) {
		this.serviciosAdicionales = serviciosAdicionales;
	}

	/**
	 * Devuelve todos los servicios adicionales que ofrece el alojamiento.
	 *
	 * @return todos los servicios adicionales que ofrece el alojamiento
	 */
	public List<ServicioAdicionalDTO> getServiciosDisponibles() {
		return serviciosDisponibles;
	}

	/**
	 * Asigna todos los servicios adicionales que ofrece el alojamiento.
	 *
	 * @param serviciosDisponibles todos los servicios adicionales que ofrece el alojamiento
	 */
	public void setServiciosDisponibles(List<ServicioAdicionalDTO> serviciosDisponibles) {
		this.serviciosDisponibles = serviciosDisponibles;
	}

	/**
	 * Devuelve el momento en que la cotización deja de servir para reservar.
	 *
	 * @return momento en que la cotización deja de servir para reservar
	 */
	public LocalDateTime getFechaExpiracion() {
		return fechaExpiracion;
	}

	/**
	 * Asigna el momento en que la cotización deja de servir para reservar.
	 *
	 * @param fechaExpiracion momento en que la cotización deja de servir para reservar
	 */
	public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
		this.fechaExpiracion = fechaExpiracion;
	}

	/**
	 * Devuelve la reserva creada con esta cotización; {@code null} si aún no se usa.
	 *
	 * @return reserva creada con esta cotización; {@code null} si aún no se usa
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva creada con esta cotización; {@code null} si aún no se usa.
	 *
	 * @param idReserva reserva creada con esta cotización; {@code null} si aún no se usa
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
	}

	/**
	 * Devuelve el huésped que pidió la cotización.
	 *
	 * @return huésped que pidió la cotización
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el huésped que pidió la cotización.
	 *
	 * @param idUsuario huésped que pidió la cotización
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el momento en que se cotizó.
	 *
	 * @return momento en que se cotizó
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que se cotizó.
	 *
	 * @param fechaCreacion momento en que se cotizó
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	/**
	 * Devuelve los servicios adicionales elegidos en JSON, con nombre y costo congelados.
	 *
	 * @return servicios adicionales elegidos en JSON, con nombre y costo congelados
	 */
	public String getDesglose() {
		return desglose;
	}

	/**
	 * Asigna los servicios adicionales elegidos en JSON, con nombre y costo congelados.
	 *
	 * @param desglose servicios adicionales elegidos en JSON, con nombre y costo congelados
	 */
	public void setDesglose(String desglose) {
		this.desglose = desglose;
	}
}
