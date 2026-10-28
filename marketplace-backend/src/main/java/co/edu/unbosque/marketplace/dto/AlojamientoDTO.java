package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFormat;

import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;
import co.edu.unbosque.marketplace.entity.EstadoUsuario;

/**
 * Datos de un alojamiento. De entrada se ignoran id, estado, idAnfitrion y
 * fechaCreacion: los asigna el service.
 *
 * @see "RF-017, RF-018, RF-019"
 */
public class AlojamientoDTO {
	private Long idAlojamiento;
	private String titulo;
	private String descripcion;
	private String ubicacion;
	private String ciudad;
	private Double latitud;
	private Double longitud;
	private String tipo;
	private Integer capacidad;
	private BigDecimal precioNoche;
	private BigDecimal porcentajeLimpieza;
	private BigDecimal porcentajeServicio;
	private String reglas;
	private EstadoAlojamiento estado;
	private Long idAnfitrion;
	private Long idPolitica;
	private String nombrePolitica;
	private LocalDateTime fechaCreacion;
	private Set<String> amenidades = new HashSet<>();
	/**
	 * URL de las imágenes, la principal primero. Solo salida: las imágenes se suben
	 * como archivo al crear o con {@code POST /alojamientos/{id}/imagenes}.
	 */
	private List<String> imagenes;
	private List<ServicioAdicionalDTO> serviciosAdicionales;
	private Integer habitaciones;
	private Integer banos;
	/** Hora de entrada (check-in), formato HH:mm. */
	@JsonFormat(pattern = "HH:mm")
	private LocalTime horaEntrada;
	/** Hora de salida (check-out), formato HH:mm. */
	@JsonFormat(pattern = "HH:mm")
	private LocalTime horaSalida;
	private LocalDateTime fechaActualizacion;
	/** Imágenes con id y portada; la principal va primero. Solo salida. */
	private List<ImagenDTO> detalleImagenes;
	/**
	 * Promedio de las reseñas visibles (una cifra decimal); null si no tiene. Solo
	 * salida.
	 */
	private Double promedioCalificacion;
	/** Cantidad de reseñas visibles. Solo salida. */
	private Long cantidadResenas;
	/** Reservas no canceladas del alojamiento. Solo salida. */
	private Long cantidadReservas;
	/** Solo salida. */
	private String nombreAnfitrion;
	/** Solo salida y solo para el administrador. */
	private String correoAnfitrion;
	/** Solo salida y solo para el administrador. */
	private EstadoUsuario estadoAnfitrion;

	public AlojamientoDTO() {
	}

	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	public String getUbicacion() {
		return ubicacion;
	}

	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public Double getLatitud() {
		return latitud;
	}

	public void setLatitud(Double latitud) {
		this.latitud = latitud;
	}

	public Double getLongitud() {
		return longitud;
	}

	public void setLongitud(Double longitud) {
		this.longitud = longitud;
	}

	public String getTipo() {
		return tipo;
	}

	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	public Integer getCapacidad() {
		return capacidad;
	}

	public void setCapacidad(Integer capacidad) {
		this.capacidad = capacidad;
	}

	public BigDecimal getPrecioNoche() {
		return precioNoche;
	}

	public void setPrecioNoche(BigDecimal precioNoche) {
		this.precioNoche = precioNoche;
	}

	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	public String getReglas() {
		return reglas;
	}

	public void setReglas(String reglas) {
		this.reglas = reglas;
	}

	public EstadoAlojamiento getEstado() {
		return estado;
	}

	public void setEstado(EstadoAlojamiento estado) {
		this.estado = estado;
	}

	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	public Long getIdPolitica() {
		return idPolitica;
	}

	public void setIdPolitica(Long idPolitica) {
		this.idPolitica = idPolitica;
	}

	public String getNombrePolitica() {
		return nombrePolitica;
	}

	public void setNombrePolitica(String nombrePolitica) {
		this.nombrePolitica = nombrePolitica;
	}

	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	public Set<String> getAmenidades() {
		return amenidades;
	}

	public void setAmenidades(Set<String> amenidades) {
		this.amenidades = amenidades;
	}

	public List<String> getImagenes() {
		return imagenes;
	}

	public void setImagenes(List<String> imagenes) {
		this.imagenes = imagenes;
	}

	public List<ServicioAdicionalDTO> getServiciosAdicionales() {
		return serviciosAdicionales;
	}

	public void setServiciosAdicionales(List<ServicioAdicionalDTO> serviciosAdicionales) {
		this.serviciosAdicionales = serviciosAdicionales;
	}

	public Integer getHabitaciones() {
		return habitaciones;
	}

	public void setHabitaciones(Integer habitaciones) {
		this.habitaciones = habitaciones;
	}

	public Integer getBanos() {
		return banos;
	}

	public void setBanos(Integer banos) {
		this.banos = banos;
	}

	public LocalTime getHoraEntrada() {
		return horaEntrada;
	}

	public void setHoraEntrada(LocalTime horaEntrada) {
		this.horaEntrada = horaEntrada;
	}

	public LocalTime getHoraSalida() {
		return horaSalida;
	}

	public void setHoraSalida(LocalTime horaSalida) {
		this.horaSalida = horaSalida;
	}

	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}

	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

	public List<ImagenDTO> getDetalleImagenes() {
		return detalleImagenes;
	}

	public void setDetalleImagenes(List<ImagenDTO> detalleImagenes) {
		this.detalleImagenes = detalleImagenes;
	}

	public Double getPromedioCalificacion() {
		return promedioCalificacion;
	}

	public void setPromedioCalificacion(Double promedioCalificacion) {
		this.promedioCalificacion = promedioCalificacion;
	}

	public Long getCantidadResenas() {
		return cantidadResenas;
	}

	public void setCantidadResenas(Long cantidadResenas) {
		this.cantidadResenas = cantidadResenas;
	}

	public Long getCantidadReservas() {
		return cantidadReservas;
	}

	public void setCantidadReservas(Long cantidadReservas) {
		this.cantidadReservas = cantidadReservas;
	}

	public String getNombreAnfitrion() {
		return nombreAnfitrion;
	}

	public void setNombreAnfitrion(String nombreAnfitrion) {
		this.nombreAnfitrion = nombreAnfitrion;
	}

	public String getCorreoAnfitrion() {
		return correoAnfitrion;
	}

	public void setCorreoAnfitrion(String correoAnfitrion) {
		this.correoAnfitrion = correoAnfitrion;
	}

	public EstadoUsuario getEstadoAnfitrion() {
		return estadoAnfitrion;
	}

	public void setEstadoAnfitrion(EstadoUsuario estadoAnfitrion) {
		this.estadoAnfitrion = estadoAnfitrion;
	}
}
