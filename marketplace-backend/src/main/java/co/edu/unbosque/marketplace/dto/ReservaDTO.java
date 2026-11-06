package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Datos de una reserva con su desglose congelado, el último pago y los datos
 * del alojamiento, del huésped y del anfitrión que muestran las pantallas de
 * reservas. Así el frontend no tiene que hacer una llamada extra por cada
 * fila.
 */
public class ReservaDTO {
	/** Código de la reserva. */
	private Long idReserva;

	/** Día de llegada. */
	private LocalDate fechaEntrada;

	/** Día de salida. */
	private LocalDate fechaSalida;

	/** Cantidad de noches. */
	private Integer noches;

	/** Cantidad de huéspedes. */
	private Integer huespedes;

	/** Estado de la reserva. */
	private EstadoReserva estado;

	/** Valor total. */
	private BigDecimal total;

	/** Desglose de precios congelado, en JSON. */
	private String desglose;

	/** Huésped que reservó. */
	private Long idHuesped;

	/** Alojamiento reservado. */
	private Long idAlojamiento;

	/** Título del alojamiento. */
	private String tituloAlojamiento;

	/** Cotización de la que salió el precio. */
	private Long idCotizacion;

	/** Momento en que se creó la reserva. */
	private LocalDateTime fechaCreacion;

	/** Indica si está confirmada y la fecha de salida ya pasó. */
	private Boolean completada;

	/** Último pago de la reserva. */
	private PagoDTO pago;
	/** URL de la imagen principal del alojamiento. */
	private String imagenAlojamiento;

	/** Ciudad del alojamiento. */
	private String ciudad;

	/** Dirección del alojamiento. */
	private String ubicacion;

	/** Tipo de alojamiento. */
	private String tipoAlojamiento;

	/** Capacidad máxima del alojamiento. */
	private Integer capacidadAlojamiento;
	/** Política de cancelación del alojamiento. */
	private String nombrePolitica;

	/** Nombre del huésped. */
	private String nombreHuesped;

	/** Anfitrión del alojamiento. */
	private Long idAnfitrion;

	/** Nombre del anfitrión. */
	private String nombreAnfitrion;
	/** Indica si el huésped ya reseñó esta reserva. */
	private Boolean tieneResena;
	/**
	 * Datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada
	 * por el huésped.
	 */
	private CancelacionDTO cancelacion;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ReservaDTO() {
	}

	/**
	 * Devuelve el código de la reserva.
	 *
	 * @return código de la reserva
	 */
	public Long getIdReserva() {
		return idReserva;
	}

	/**
	 * Asigna el código de la reserva.
	 *
	 * @param idReserva código de la reserva
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
	 * Devuelve el valor total.
	 *
	 * @return valor total
	 */
	public BigDecimal getTotal() {
		return total;
	}

	/**
	 * Asigna el valor total.
	 *
	 * @param total valor total
	 */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	/**
	 * Devuelve el desglose de precios congelado, en JSON.
	 *
	 * @return desglose de precios congelado, en JSON
	 */
	public String getDesglose() {
		return desglose;
	}

	/**
	 * Asigna el desglose de precios congelado, en JSON.
	 *
	 * @param desglose desglose de precios congelado, en JSON
	 */
	public void setDesglose(String desglose) {
		this.desglose = desglose;
	}

	/**
	 * Devuelve el huésped que reservó.
	 *
	 * @return huésped que reservó
	 */
	public Long getIdHuesped() {
		return idHuesped;
	}

	/**
	 * Asigna el huésped que reservó.
	 *
	 * @param idHuesped huésped que reservó
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
	 * Devuelve el título del alojamiento.
	 *
	 * @return título del alojamiento
	 */
	public String getTituloAlojamiento() {
		return tituloAlojamiento;
	}

	/**
	 * Asigna el título del alojamiento.
	 *
	 * @param tituloAlojamiento título del alojamiento
	 */
	public void setTituloAlojamiento(String tituloAlojamiento) {
		this.tituloAlojamiento = tituloAlojamiento;
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

	/**
	 * Devuelve {@code true} si está confirmada y la fecha de salida ya pasó.
	 *
	 * @return {@code true} si está confirmada y la fecha de salida ya pasó
	 */
	public Boolean getCompletada() {
		return completada;
	}

	/**
	 * Asigna {@code true} si está confirmada y la fecha de salida ya pasó.
	 *
	 * @param completada {@code true} si está confirmada y la fecha de salida ya pasó
	 */
	public void setCompletada(Boolean completada) {
		this.completada = completada;
	}

	/**
	 * Devuelve el último pago de la reserva.
	 *
	 * @return último pago de la reserva
	 */
	public PagoDTO getPago() {
		return pago;
	}

	/**
	 * Asigna el último pago de la reserva.
	 *
	 * @param pago último pago de la reserva
	 */
	public void setPago(PagoDTO pago) {
		this.pago = pago;
	}

	/**
	 * Devuelve la URL de la imagen principal del alojamiento.
	 *
	 * @return URL de la imagen principal del alojamiento
	 */
	public String getImagenAlojamiento() {
		return imagenAlojamiento;
	}

	/**
	 * Asigna la URL de la imagen principal del alojamiento.
	 *
	 * @param imagenAlojamiento URL de la imagen principal del alojamiento
	 */
	public void setImagenAlojamiento(String imagenAlojamiento) {
		this.imagenAlojamiento = imagenAlojamiento;
	}

	/**
	 * Devuelve la ciudad del alojamiento.
	 *
	 * @return ciudad del alojamiento
	 */
	public String getCiudad() {
		return ciudad;
	}

	/**
	 * Asigna la ciudad del alojamiento.
	 *
	 * @param ciudad ciudad del alojamiento
	 */
	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	/**
	 * Devuelve la dirección del alojamiento.
	 *
	 * @return dirección del alojamiento
	 */
	public String getUbicacion() {
		return ubicacion;
	}

	/**
	 * Asigna la dirección del alojamiento.
	 *
	 * @param ubicacion dirección del alojamiento
	 */
	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	/**
	 * Devuelve el tipo de alojamiento.
	 *
	 * @return tipo de alojamiento
	 */
	public String getTipoAlojamiento() {
		return tipoAlojamiento;
	}

	/**
	 * Asigna el tipo de alojamiento.
	 *
	 * @param tipoAlojamiento tipo de alojamiento
	 */
	public void setTipoAlojamiento(String tipoAlojamiento) {
		this.tipoAlojamiento = tipoAlojamiento;
	}

	/**
	 * Devuelve la capacidad máxima del alojamiento.
	 *
	 * @return capacidad máxima del alojamiento
	 */
	public Integer getCapacidadAlojamiento() {
		return capacidadAlojamiento;
	}

	/**
	 * Asigna la capacidad máxima del alojamiento.
	 *
	 * @param capacidadAlojamiento capacidad máxima del alojamiento
	 */
	public void setCapacidadAlojamiento(Integer capacidadAlojamiento) {
		this.capacidadAlojamiento = capacidadAlojamiento;
	}

	/**
	 * Devuelve la política de cancelación del alojamiento.
	 *
	 * @return política de cancelación del alojamiento
	 */
	public String getNombrePolitica() {
		return nombrePolitica;
	}

	/**
	 * Asigna la política de cancelación del alojamiento.
	 *
	 * @param nombrePolitica política de cancelación del alojamiento
	 */
	public void setNombrePolitica(String nombrePolitica) {
		this.nombrePolitica = nombrePolitica;
	}

	/**
	 * Devuelve el nombre del huésped.
	 *
	 * @return nombre del huésped
	 */
	public String getNombreHuesped() {
		return nombreHuesped;
	}

	/**
	 * Asigna el nombre del huésped.
	 *
	 * @param nombreHuesped nombre del huésped
	 */
	public void setNombreHuesped(String nombreHuesped) {
		this.nombreHuesped = nombreHuesped;
	}

	/**
	 * Devuelve el anfitrión del alojamiento.
	 *
	 * @return anfitrión del alojamiento
	 */
	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	/**
	 * Asigna el anfitrión del alojamiento.
	 *
	 * @param idAnfitrion anfitrión del alojamiento
	 */
	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	/**
	 * Devuelve el nombre del anfitrión.
	 *
	 * @return nombre del anfitrión
	 */
	public String getNombreAnfitrion() {
		return nombreAnfitrion;
	}

	/**
	 * Asigna el nombre del anfitrión.
	 *
	 * @param nombreAnfitrion nombre del anfitrión
	 */
	public void setNombreAnfitrion(String nombreAnfitrion) {
		this.nombreAnfitrion = nombreAnfitrion;
	}

	/**
	 * Devuelve {@code true} si el huésped ya reseñó esta reserva.
	 *
	 * @return {@code true} si el huésped ya reseñó esta reserva
	 */
	public Boolean getTieneResena() {
		return tieneResena;
	}

	/**
	 * Asigna {@code true} si el huésped ya reseñó esta reserva.
	 *
	 * @param tieneResena {@code true} si el huésped ya reseñó esta reserva
	 */
	public void setTieneResena(Boolean tieneResena) {
		this.tieneResena = tieneResena;
	}

	/**
	 * Devuelve los datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada por el huésped.
	 *
	 * @return datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada por el huésped
	 */
	public CancelacionDTO getCancelacion() {
		return cancelacion;
	}

	/**
	 * Asigna los datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada por el huésped.
	 *
	 * @param cancelacion datos de la cancelación (fecha, motivo, devolución); null si no fue cancelada por el huésped
	 */
	public void setCancelacion(CancelacionDTO cancelacion) {
		this.cancelacion = cancelacion;
	}
}
