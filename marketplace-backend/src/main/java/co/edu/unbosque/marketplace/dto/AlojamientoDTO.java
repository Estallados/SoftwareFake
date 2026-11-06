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
 * Datos de un alojamiento tal como entran y salen por la API. Al crear o
 * editar se ignoran el id, el estado, el anfitrión y la fecha de creación,
 * porque los asigna el servidor. Los campos marcados como "solo salida" los
 * llena el servidor al responder.
 */
public class AlojamientoDTO {
	/** Identificador del alojamiento. */
	private Long idAlojamiento;

	/** Título de la publicación. */
	private String titulo;

	/** Descripción del alojamiento. */
	private String descripcion;

	/** Dirección o referencia de la ubicación. */
	private String ubicacion;

	/** Ciudad donde queda. */
	private String ciudad;

	/** Latitud para el mapa; es opcional. */
	private Double latitud;

	/** Longitud para el mapa; es opcional. */
	private Double longitud;

	/** Tipo de alojamiento, por ejemplo Casa. */
	private String tipo;

	/** Cantidad máxima de huéspedes. */
	private Integer capacidad;

	/** Precio por noche, en pesos colombianos. */
	private BigDecimal precioNoche;

	/** Porcentaje propio de limpieza; si es {@code null} se usa el global. */
	private BigDecimal porcentajeLimpieza;

	/** Porcentaje propio de servicio; si es {@code null} se usa el global. */
	private BigDecimal porcentajeServicio;

	/** Reglas de la casa. */
	private String reglas;

	/** Estado de la publicación. */
	private EstadoAlojamiento estado;

	/** Anfitrión dueño. */
	private Long idAnfitrion;

	/** Política de cancelación elegida. */
	private Long idPolitica;

	/** Nombre de la política de cancelación. */
	private String nombrePolitica;

	/** Momento en que se creó la publicación. */
	private LocalDateTime fechaCreacion;

	/** Amenidades del alojamiento, por ejemplo wifi o piscina. */
	private Set<String> amenidades = new HashSet<>();
	/**
	 * URL de las imágenes, la principal primero. Solo salida: las imágenes se suben
	 * como archivo al crear o con {@code POST /alojamientos/{id}/imagenes}.
	 */
	private List<String> imagenes;

	/** Servicios adicionales que ofrece. */
	private List<ServicioAdicionalDTO> serviciosAdicionales;

	/** Cantidad de habitaciones. */
	private Integer habitaciones;

	/** Cantidad de camas. */
	private Integer camas;

	/** Cantidad de baños. */
	private Integer banos;
	/** Hora de entrada (check-in), formato HH:mm. */
	@JsonFormat(pattern = "HH:mm")
	private LocalTime horaEntrada;
	/** Hora de salida (check-out), formato HH:mm. */
	@JsonFormat(pattern = "HH:mm")
	private LocalTime horaSalida;

	/** Última edición de los datos o de las imágenes. */
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

	/** Nombre del anfitrión. Solo salida. */
	private String nombreAnfitrion;

	/** Correo del anfitrión. Solo salida y solo para el administrador. */
	private String correoAnfitrion;

	/**
	 * Estado de la cuenta del anfitrión. Solo salida y solo para el
	 * administrador.
	 */
	private EstadoUsuario estadoAnfitrion;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public AlojamientoDTO() {
	}

	/**
	 * Devuelve el identificador del alojamiento.
	 *
	 * @return identificador del alojamiento
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el identificador del alojamiento.
	 *
	 * @param idAlojamiento identificador del alojamiento
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve el título de la publicación.
	 *
	 * @return título de la publicación
	 */
	public String getTitulo() {
		return titulo;
	}

	/**
	 * Asigna el título de la publicación.
	 *
	 * @param titulo título de la publicación
	 */
	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	/**
	 * Devuelve la descripción del alojamiento.
	 *
	 * @return descripción del alojamiento
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna la descripción del alojamiento.
	 *
	 * @param descripcion descripción del alojamiento
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	/**
	 * Devuelve la dirección o referencia de la ubicación.
	 *
	 * @return dirección o referencia de la ubicación
	 */
	public String getUbicacion() {
		return ubicacion;
	}

	/**
	 * Asigna la dirección o referencia de la ubicación.
	 *
	 * @param ubicacion dirección o referencia de la ubicación
	 */
	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	/**
	 * Devuelve la ciudad donde queda.
	 *
	 * @return ciudad donde queda
	 */
	public String getCiudad() {
		return ciudad;
	}

	/**
	 * Asigna la ciudad donde queda.
	 *
	 * @param ciudad ciudad donde queda
	 */
	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	/**
	 * Devuelve la latitud para el mapa; es opcional.
	 *
	 * @return latitud para el mapa; es opcional
	 */
	public Double getLatitud() {
		return latitud;
	}

	/**
	 * Asigna la latitud para el mapa; es opcional.
	 *
	 * @param latitud latitud para el mapa; es opcional
	 */
	public void setLatitud(Double latitud) {
		this.latitud = latitud;
	}

	/**
	 * Devuelve la longitud para el mapa; es opcional.
	 *
	 * @return longitud para el mapa; es opcional
	 */
	public Double getLongitud() {
		return longitud;
	}

	/**
	 * Asigna la longitud para el mapa; es opcional.
	 *
	 * @param longitud longitud para el mapa; es opcional
	 */
	public void setLongitud(Double longitud) {
		this.longitud = longitud;
	}

	/**
	 * Devuelve el tipo de alojamiento, por ejemplo Casa.
	 *
	 * @return tipo de alojamiento, por ejemplo Casa
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Asigna el tipo de alojamiento, por ejemplo Casa.
	 *
	 * @param tipo tipo de alojamiento, por ejemplo Casa
	 */
	public void setTipo(String tipo) {
		this.tipo = tipo;
	}

	/**
	 * Devuelve la cantidad máxima de huéspedes.
	 *
	 * @return cantidad máxima de huéspedes
	 */
	public Integer getCapacidad() {
		return capacidad;
	}

	/**
	 * Asigna la cantidad máxima de huéspedes.
	 *
	 * @param capacidad cantidad máxima de huéspedes
	 */
	public void setCapacidad(Integer capacidad) {
		this.capacidad = capacidad;
	}

	/**
	 * Devuelve el precio por noche, en pesos colombianos.
	 *
	 * @return precio por noche, en pesos colombianos
	 */
	public BigDecimal getPrecioNoche() {
		return precioNoche;
	}

	/**
	 * Asigna el precio por noche, en pesos colombianos.
	 *
	 * @param precioNoche precio por noche, en pesos colombianos
	 */
	public void setPrecioNoche(BigDecimal precioNoche) {
		this.precioNoche = precioNoche;
	}

	/**
	 * Devuelve el porcentaje propio de limpieza; si es {@code null} se usa el global.
	 *
	 * @return porcentaje propio de limpieza; si es {@code null} se usa el global
	 */
	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	/**
	 * Asigna el porcentaje propio de limpieza; si es {@code null} se usa el global.
	 *
	 * @param porcentajeLimpieza porcentaje propio de limpieza; si es {@code null} se usa el global
	 */
	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	/**
	 * Devuelve el porcentaje propio de servicio; si es {@code null} se usa el global.
	 *
	 * @return porcentaje propio de servicio; si es {@code null} se usa el global
	 */
	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	/**
	 * Asigna el porcentaje propio de servicio; si es {@code null} se usa el global.
	 *
	 * @param porcentajeServicio porcentaje propio de servicio; si es {@code null} se usa el global
	 */
	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	/**
	 * Devuelve las reglas de la casa.
	 *
	 * @return reglas de la casa
	 */
	public String getReglas() {
		return reglas;
	}

	/**
	 * Asigna las reglas de la casa.
	 *
	 * @param reglas reglas de la casa
	 */
	public void setReglas(String reglas) {
		this.reglas = reglas;
	}

	/**
	 * Devuelve el estado de la publicación.
	 *
	 * @return estado de la publicación
	 */
	public EstadoAlojamiento getEstado() {
		return estado;
	}

	/**
	 * Asigna el estado de la publicación.
	 *
	 * @param estado estado de la publicación
	 */
	public void setEstado(EstadoAlojamiento estado) {
		this.estado = estado;
	}

	/**
	 * Devuelve el anfitrión dueño.
	 *
	 * @return anfitrión dueño
	 */
	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	/**
	 * Asigna el anfitrión dueño.
	 *
	 * @param idAnfitrion anfitrión dueño
	 */
	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	/**
	 * Devuelve la política de cancelación elegida.
	 *
	 * @return política de cancelación elegida
	 */
	public Long getIdPolitica() {
		return idPolitica;
	}

	/**
	 * Asigna la política de cancelación elegida.
	 *
	 * @param idPolitica política de cancelación elegida
	 */
	public void setIdPolitica(Long idPolitica) {
		this.idPolitica = idPolitica;
	}

	/**
	 * Devuelve el nombre de la política de cancelación.
	 *
	 * @return nombre de la política de cancelación
	 */
	public String getNombrePolitica() {
		return nombrePolitica;
	}

	/**
	 * Asigna el nombre de la política de cancelación.
	 *
	 * @param nombrePolitica nombre de la política de cancelación
	 */
	public void setNombrePolitica(String nombrePolitica) {
		this.nombrePolitica = nombrePolitica;
	}

	/**
	 * Devuelve el momento en que se creó la publicación.
	 *
	 * @return momento en que se creó la publicación
	 */
	public LocalDateTime getFechaCreacion() {
		return fechaCreacion;
	}

	/**
	 * Asigna el momento en que se creó la publicación.
	 *
	 * @param fechaCreacion momento en que se creó la publicación
	 */
	public void setFechaCreacion(LocalDateTime fechaCreacion) {
		this.fechaCreacion = fechaCreacion;
	}

	/**
	 * Devuelve las amenidades del alojamiento, por ejemplo wifi o piscina.
	 *
	 * @return amenidades del alojamiento, por ejemplo wifi o piscina
	 */
	public Set<String> getAmenidades() {
		return amenidades;
	}

	/**
	 * Asigna las amenidades del alojamiento, por ejemplo wifi o piscina.
	 *
	 * @param amenidades amenidades del alojamiento, por ejemplo wifi o piscina
	 */
	public void setAmenidades(Set<String> amenidades) {
		this.amenidades = amenidades;
	}

	/**
	 * Devuelve la URL de las imágenes, la principal primero.
	 *
	 * @return URL de las imágenes, la principal primero
	 */
	public List<String> getImagenes() {
		return imagenes;
	}

	/**
	 * Asigna la URL de las imágenes, la principal primero.
	 *
	 * @param imagenes URL de las imágenes, la principal primero
	 */
	public void setImagenes(List<String> imagenes) {
		this.imagenes = imagenes;
	}

	/**
	 * Devuelve los servicios adicionales que ofrece.
	 *
	 * @return servicios adicionales que ofrece
	 */
	public List<ServicioAdicionalDTO> getServiciosAdicionales() {
		return serviciosAdicionales;
	}

	/**
	 * Asigna los servicios adicionales que ofrece.
	 *
	 * @param serviciosAdicionales servicios adicionales que ofrece
	 */
	public void setServiciosAdicionales(List<ServicioAdicionalDTO> serviciosAdicionales) {
		this.serviciosAdicionales = serviciosAdicionales;
	}

	/**
	 * Devuelve la cantidad de habitaciones.
	 *
	 * @return cantidad de habitaciones
	 */
	public Integer getHabitaciones() {
		return habitaciones;
	}

	/**
	 * Asigna la cantidad de habitaciones.
	 *
	 * @param habitaciones cantidad de habitaciones
	 */
	public void setHabitaciones(Integer habitaciones) {
		this.habitaciones = habitaciones;
	}

	/**
	 * Devuelve la cantidad de camas.
	 *
	 * @return cantidad de camas
	 */
	public Integer getCamas() {
		return camas;
	}

	/**
	 * Asigna la cantidad de camas.
	 *
	 * @param camas cantidad de camas
	 */
	public void setCamas(Integer camas) {
		this.camas = camas;
	}

	/**
	 * Devuelve la cantidad de baños.
	 *
	 * @return cantidad de baños
	 */
	public Integer getBanos() {
		return banos;
	}

	/**
	 * Asigna la cantidad de baños.
	 *
	 * @param banos cantidad de baños
	 */
	public void setBanos(Integer banos) {
		this.banos = banos;
	}

	/**
	 * Devuelve la hora de entrada (check-in), formato HH:mm.
	 *
	 * @return hora de entrada (check-in), formato HH:mm
	 */
	public LocalTime getHoraEntrada() {
		return horaEntrada;
	}

	/**
	 * Asigna la hora de entrada (check-in), formato HH:mm.
	 *
	 * @param horaEntrada hora de entrada (check-in), formato HH:mm
	 */
	public void setHoraEntrada(LocalTime horaEntrada) {
		this.horaEntrada = horaEntrada;
	}

	/**
	 * Devuelve la hora de salida (check-out), formato HH:mm.
	 *
	 * @return hora de salida (check-out), formato HH:mm
	 */
	public LocalTime getHoraSalida() {
		return horaSalida;
	}

	/**
	 * Asigna la hora de salida (check-out), formato HH:mm.
	 *
	 * @param horaSalida hora de salida (check-out), formato HH:mm
	 */
	public void setHoraSalida(LocalTime horaSalida) {
		this.horaSalida = horaSalida;
	}

	/**
	 * Devuelve la última edición de los datos o de las imágenes.
	 *
	 * @return última edición de los datos o de las imágenes
	 */
	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}

	/**
	 * Asigna la última edición de los datos o de las imágenes.
	 *
	 * @param fechaActualizacion última edición de los datos o de las imágenes
	 */
	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

	/**
	 * Devuelve las imágenes con id y portada; la principal va primero.
	 *
	 * @return imágenes con id y portada; la principal va primero
	 */
	public List<ImagenDTO> getDetalleImagenes() {
		return detalleImagenes;
	}

	/**
	 * Asigna las imágenes con id y portada; la principal va primero.
	 *
	 * @param detalleImagenes imágenes con id y portada; la principal va primero
	 */
	public void setDetalleImagenes(List<ImagenDTO> detalleImagenes) {
		this.detalleImagenes = detalleImagenes;
	}

	/**
	 * Devuelve el promedio de las reseñas visibles (una cifra decimal); null si no tiene.
	 *
	 * @return promedio de las reseñas visibles (una cifra decimal); null si no tiene
	 */
	public Double getPromedioCalificacion() {
		return promedioCalificacion;
	}

	/**
	 * Asigna el promedio de las reseñas visibles (una cifra decimal); null si no tiene.
	 *
	 * @param promedioCalificacion promedio de las reseñas visibles (una cifra decimal); null si no tiene
	 */
	public void setPromedioCalificacion(Double promedioCalificacion) {
		this.promedioCalificacion = promedioCalificacion;
	}

	/**
	 * Devuelve la cantidad de reseñas visibles.
	 *
	 * @return cantidad de reseñas visibles
	 */
	public Long getCantidadResenas() {
		return cantidadResenas;
	}

	/**
	 * Asigna la cantidad de reseñas visibles.
	 *
	 * @param cantidadResenas cantidad de reseñas visibles
	 */
	public void setCantidadResenas(Long cantidadResenas) {
		this.cantidadResenas = cantidadResenas;
	}

	/**
	 * Devuelve las reservas no canceladas del alojamiento.
	 *
	 * @return reservas no canceladas del alojamiento
	 */
	public Long getCantidadReservas() {
		return cantidadReservas;
	}

	/**
	 * Asigna las reservas no canceladas del alojamiento.
	 *
	 * @param cantidadReservas reservas no canceladas del alojamiento
	 */
	public void setCantidadReservas(Long cantidadReservas) {
		this.cantidadReservas = cantidadReservas;
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
	 * Devuelve el correo del anfitrión.
	 *
	 * @return correo del anfitrión
	 */
	public String getCorreoAnfitrion() {
		return correoAnfitrion;
	}

	/**
	 * Asigna el correo del anfitrión.
	 *
	 * @param correoAnfitrion correo del anfitrión
	 */
	public void setCorreoAnfitrion(String correoAnfitrion) {
		this.correoAnfitrion = correoAnfitrion;
	}

	/**
	 * Devuelve el estado de la cuenta del anfitrión.
	 *
	 * @return estado de la cuenta del anfitrión
	 */
	public EstadoUsuario getEstadoAnfitrion() {
		return estadoAnfitrion;
	}

	/**
	 * Asigna el estado de la cuenta del anfitrión.
	 *
	 * @param estadoAnfitrion estado de la cuenta del anfitrión
	 */
	public void setEstadoAnfitrion(EstadoUsuario estadoAnfitrion) {
		this.estadoAnfitrion = estadoAnfitrion;
	}
}
