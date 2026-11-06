package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Cotización: el precio calculado para una estancia antes de reservar.
 * <p>
 * Guarda cada componente del precio y los porcentajes que estaban vigentes al
 * cotizar. Una reserva solo puede crearse desde una cotización vigente (que no
 * haya vencido) y que no se haya usado antes.
 * </p>
 * <p>
 * Cálculo: precio base = precio por noche × noches; a eso se le resta el
 * descuento y se le suman la limpieza, el servicio y los servicios
 * adicionales.
 * </p>
 */
@Entity
@Table(name = "cotizacion")
public class Cotizacion {

	/** Identificador de la cotización. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idCotizacion;

	/** Día de llegada. */
	private LocalDate fechaEntrada;

	/** Día de salida. */
	private LocalDate fechaSalida;

	/** Cantidad de huéspedes. */
	private Integer huespedes;

	/** Cantidad de noches. */
	private Integer noches;

	/** Precio por noche del alojamiento al momento de cotizar. */
	private BigDecimal precioNoche;

	/** Precio por noche multiplicado por las noches. */
	private BigDecimal precioBase;

	/** Descuento aplicado al precio base. */
	private BigDecimal descuento;

	/** Porcentaje de limpieza que se aplicó. */
	private BigDecimal porcentajeLimpieza;

	/** Valor de la limpieza. */
	private BigDecimal valorLimpieza;

	/** Porcentaje de la tarifa de servicio que se aplicó. */
	private BigDecimal porcentajeServicio;

	/** Valor de la tarifa de servicio. */
	private BigDecimal valorServicio;

	/** Suma de los servicios adicionales elegidos. */
	private BigDecimal valorAdicionales;

	/** Valor total a pagar. */
	private BigDecimal total;

	/** Servicios adicionales elegidos, en JSON con nombre y costo congelados. */
	private String desglose;

	/** Huésped que pidió la cotización. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Alojamiento cotizado. */
	@Column(name = "id_alojamiento")
	private Long idAlojamiento;

	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;

	/** Reserva que se creó con esta cotización; {@code null} si aún no se usa. */
	@Column(name = "id_reserva")
	private Long idReserva;

	/**
	 * Clave foránea hacia {@link Reserva}; el valor se asigna con
	 * {@code idReserva}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_reserva", insertable = false, updatable = false)
	private Reserva reserva;

	/** Momento en que se cotizó. */
	private LocalDateTime fechaCreacion;

	/** Momento en que la cotización deja de servir para reservar. */
	private LocalDateTime fechaExpiracion;

	/** Constructor vacío, requerido por JPA. */
	public Cotizacion() {
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
	 * Devuelve el precio por noche del alojamiento al momento de cotizar.
	 *
	 * @return precio por noche del alojamiento al momento de cotizar
	 */
	public BigDecimal getPrecioNoche() {
		return precioNoche;
	}

	/**
	 * Asigna el precio por noche del alojamiento al momento de cotizar.
	 *
	 * @param precioNoche precio por noche del alojamiento al momento de cotizar
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
	 * Devuelve el descuento aplicado al precio base.
	 *
	 * @return descuento aplicado al precio base
	 */
	public BigDecimal getDescuento() {
		return descuento;
	}

	/**
	 * Asigna el descuento aplicado al precio base.
	 *
	 * @param descuento descuento aplicado al precio base
	 */
	public void setDescuento(BigDecimal descuento) {
		this.descuento = descuento;
	}

	/**
	 * Devuelve el porcentaje de limpieza que se aplicó.
	 *
	 * @return porcentaje de limpieza que se aplicó
	 */
	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	/**
	 * Asigna el porcentaje de limpieza que se aplicó.
	 *
	 * @param porcentajeLimpieza porcentaje de limpieza que se aplicó
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
	 * Devuelve el porcentaje de la tarifa de servicio que se aplicó.
	 *
	 * @return porcentaje de la tarifa de servicio que se aplicó
	 */
	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	/**
	 * Asigna el porcentaje de la tarifa de servicio que se aplicó.
	 *
	 * @param porcentajeServicio porcentaje de la tarifa de servicio que se aplicó
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
	 * Devuelve los servicios adicionales elegidos, en JSON con nombre y costo congelados.
	 *
	 * @return servicios adicionales elegidos, en JSON con nombre y costo congelados
	 */
	public String getDesglose() {
		return desglose;
	}

	/**
	 * Asigna los servicios adicionales elegidos, en JSON con nombre y costo congelados.
	 *
	 * @param desglose servicios adicionales elegidos, en JSON con nombre y costo congelados
	 */
	public void setDesglose(String desglose) {
		this.desglose = desglose;
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
	 * Devuelve el alojamiento cotizado.
	 *
	 * @return alojamiento cotizado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento cotizado.
	 *
	 * @param idAlojamiento alojamiento cotizado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve la reserva que se creó con esta cotización; {@code null} si aún no se usa.
	 *
	 * @return reserva que se creó con esta cotización; {@code null} si aún no se usa
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna la reserva que se creó con esta cotización; {@code null} si aún no se usa.
	 *
	 * @param idReserva reserva que se creó con esta cotización; {@code null} si aún no se usa
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
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
}
