package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Reserva de un alojamiento hecha por un huésped.
 * <p>
 * El desglose de precios se congela al crearla (es una copia de la cotización),
 * así que los cambios posteriores de tarifas o de servicios adicionales no la
 * alteran.
 * </p>
 */
@Entity
@Table(name = "reserva")
public class Reserva {

	/** Identificador de la reserva; es también su código. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idReserva;

	/** Día de llegada. */
	private LocalDate fechaEntrada;

	/** Día de salida (no ocupa noche). */
	private LocalDate fechaSalida;

	/** Cantidad de noches entre la entrada y la salida. */
	private Integer noches;

	/** Cantidad de huéspedes. */
	private Integer huespedes;

	/** Estado de la reserva. */
	@Enumerated(EnumType.STRING)
	private EstadoReserva estado;

	/** Valor total a pagar, en pesos colombianos. */
	private BigDecimal total;

	/** Desglose congelado en JSON (copia de la cotización usada). */
	private String desglose;

	/** Huésped que hizo la reserva. */
	@Column(name = "id_huesped")
	private Long idHuesped;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idHuesped}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_huesped", insertable = false, updatable = false)
	private Usuario huesped;

	/** Alojamiento reservado. */
	@Column(name = "id_alojamiento")
	private Long idAlojamiento;

	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;

	/** Cotización de la que salió el precio. */
	@Column(name = "id_cotizacion")
	private Long idCotizacion;

	/**
	 * Clave foránea hacia {@link Cotizacion}; el valor se asigna con
	 * {@code idCotizacion}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_cotizacion", insertable = false, updatable = false)
	private Cotizacion cotizacion;

	/** Momento en que se creó la reserva. */
	private LocalDateTime fechaCreacion;

	/** Constructor vacío, requerido por JPA. */
	public Reserva() {
	}

	/**
	 * Devuelve el identificador de la reserva; es también su código.
	 *
	 * @return identificador de la reserva; es también su código
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna el identificador de la reserva; es también su código.
	 *
	 * @param idReserva identificador de la reserva; es también su código
	 */
	public void setIdReserva(Long idReserva) {
		this.idReserva = idReserva;
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
	 * Devuelve el día de salida (no ocupa noche).
	 *
	 * @return día de salida (no ocupa noche)
	 */
	public LocalDate getFechaSalida() {
		return fechaSalida;
	}

	/**
	 * Asigna el día de salida (no ocupa noche).
	 *
	 * @param fechaSalida día de salida (no ocupa noche)
	 */
	public void setFechaSalida(LocalDate fechaSalida) {
		this.fechaSalida = fechaSalida;
	}

	/**
	 * Devuelve la cantidad de noches entre la entrada y la salida.
	 *
	 * @return cantidad de noches entre la entrada y la salida
	 */
	public Integer getNoches() {
		return noches;
	}

	/**
	 * Asigna la cantidad de noches entre la entrada y la salida.
	 *
	 * @param noches cantidad de noches entre la entrada y la salida
	 */
	public void setNoches(Integer noches) {
		this.noches = noches;
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
	 * Devuelve el estado de la reserva.
	 *
	 * @return estado de la reserva
	 */
	public EstadoReserva getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado de la reserva.
	 *
	 * @param estado estado de la reserva
	 */
	public void setEstado(EstadoReserva estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el valor total a pagar, en pesos colombianos.
	 *
	 * @return valor total a pagar, en pesos colombianos
	 */
	public BigDecimal getTotal() {
		return total;
	}

	/**
	 * Asigna el valor total a pagar, en pesos colombianos.
	 *
	 * @param total valor total a pagar, en pesos colombianos
	 */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	/**
	 * Devuelve el desglose congelado en JSON (copia de la cotización usada).
	 *
	 * @return desglose congelado en JSON (copia de la cotización usada)
	 */
	public String getDesglose() {
		return desglose;
	}

	/**
	 * Asigna el desglose congelado en JSON (copia de la cotización usada).
	 *
	 * @param desglose desglose congelado en JSON (copia de la cotización usada)
	 */
	public void setDesglose(String desglose) {
		this.desglose = desglose;
	}

	/**
	 * Devuelve el huésped que hizo la reserva.
	 *
	 * @return huésped que hizo la reserva
	 */
	public Long getIdHuesped() {
		return idHuesped;
	}

	/**
	 * Asigna el huésped que hizo la reserva.
	 *
	 * @param idHuesped huésped que hizo la reserva
	 */
	public void setIdHuesped(Long idHuesped) {
		this.idHuesped = idHuesped;
	}

	/**
	 * Devuelve el alojamiento reservado.
	 *
	 * @return alojamiento reservado
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento reservado.
	 *
	 * @param idAlojamiento alojamiento reservado
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve la cotización de la que salió el precio.
	 *
	 * @return cotización de la que salió el precio
	 */
	public Long getIdCotizacion() {
		return idCotizacion;
	}

	/**
	 * Asigna la cotización de la que salió el precio.
	 *
	 * @param idCotizacion cotización de la que salió el precio
	 */
	public void setIdCotizacion(Long idCotizacion) {
		this.idCotizacion = idCotizacion;
	}

	/**
	 * Devuelve el momento en que se creó la reserva.
	 *
	 * @return momento en que se creó la reserva
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que se creó la reserva.
	 *
	 * @param fechaCreacion momento en que se creó la reserva
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}
}
