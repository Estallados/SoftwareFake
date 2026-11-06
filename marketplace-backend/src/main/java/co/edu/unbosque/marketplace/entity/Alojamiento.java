package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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
 * Alojamiento: la publicación que hace un anfitrión.
 * <p>
 * Las imágenes y los servicios adicionales están en sus propias clases
 * ({@link Imagen}, {@link ServicioAdicional}); las amenidades (lo que incluye
 * el alojamiento, como wifi o piscina) son una colección de textos.
 * </p>
 * <p>
 * Las columnas que filtra la búsqueda tienen índices en la base de datos para
 * que las consultas sean rápidas.
 * </p>
 */
@Entity
@Table(name = "alojamiento")
public class Alojamiento {

	/** Identificador del alojamiento. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idAlojamiento;

	/** Título de la publicación. */
	private String titulo;

	/** Descripción que escribe el anfitrión. */
	private String descripcion;

	/** Dirección o referencia textual de la ubicación. */
	private String ubicacion;

	/** Ciudad donde queda; es el filtro principal de la búsqueda. */
	private String ciudad;

	/** Latitud para el mapa; es opcional. */
	private Double latitud;

	/** Longitud para el mapa; es opcional. */
	private Double longitud;

	/** Tipo de alojamiento, por ejemplo Casa o Apartamento. */
	private String tipo;

	/** Cantidad máxima de huéspedes. */
	private Integer capacidad;

	/** Cantidad de habitaciones. */
	private Integer habitaciones;

	/** Cantidad de camas. */
	private Integer camas;

	/** Cantidad de baños. */
	private Integer banos;

	/** Hora desde la que el huésped puede llegar (check-in). */
	private LocalTime horaEntrada;
	/** Hora límite de salida del huésped (check-out). */
	private LocalTime horaSalida;

	/** Precio por noche, en pesos colombianos. */
	private BigDecimal precioNoche;

	/**
	 * Porcentaje propio de limpieza; si es null se usa el global de
	 * ConfiguracionTarifa.
	 */
	private BigDecimal porcentajeLimpieza;
	/**
	 * Porcentaje propio de servicio; si es null se usa el global de
	 * ConfiguracionTarifa.
	 */
	private BigDecimal porcentajeServicio;

	/** Reglas de la casa, por ejemplo "No fumar". */
	private String reglas;

	/** Estado de la publicación. */
	@Enumerated(EnumType.STRING)
	private EstadoAlojamiento estado;

	/** Anfitrión dueño del alojamiento. */
	@Column(name = "id_anfitrion")
	private Long idAnfitrion;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idAnfitrion}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_anfitrion", insertable = false, updatable = false)
	private Usuario anfitrion;

	/** Política de cancelación que eligió el anfitrión. */
	@Column(name = "id_politica")
	private Long idPolitica;

	/**
	 * Clave foránea hacia {@link PoliticaCancelacion}; el valor se asigna con
	 * {@code idPolitica}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_politica", insertable = false, updatable = false)
	private PoliticaCancelacion politica;

	/** Momento en que se creó la publicación. */
	private LocalDateTime fechaCreacion;

	/** Última edición de los datos o de las imágenes de la publicación. */
	private LocalDateTime fechaActualizacion;
	/**
	 * Amenidades del alojamiento (wifi, piscina, cocina...). En la base de datos
	 * la clave primaria es (id_alojamiento, amenidad): un alojamiento no repite
	 * amenidades.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "alojamientoAmenidad", joinColumns = @JoinColumn(name = "id_alojamiento"))
	@Column(name = "amenidad")
	private Set<String> amenidades = new HashSet<>();

	/** Constructor vacío, requerido por JPA. */
	public Alojamiento() {
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
	 * Devuelve la descripción que escribe el anfitrión.
	 *
	 * @return descripción que escribe el anfitrión
	 */
	public String getDescripcion() {
		return descripcion;
	}

	/**
	 * Asigna la descripción que escribe el anfitrión.
	 *
	 * @param descripcion descripción que escribe el anfitrión
	 */
	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

	/**
	 * Devuelve la dirección o referencia textual de la ubicación.
	 *
	 * @return dirección o referencia textual de la ubicación
	 */
	public String getUbicacion() {
		return ubicacion;
	}

	/**
	 * Asigna la dirección o referencia textual de la ubicación.
	 *
	 * @param ubicacion dirección o referencia textual de la ubicación
	 */
	public void setUbicacion(String ubicacion) {
		this.ubicacion = ubicacion;
	}

	/**
	 * Devuelve la ciudad donde queda; es el filtro principal de la búsqueda.
	 *
	 * @return ciudad donde queda; es el filtro principal de la búsqueda
	 */
	public String getCiudad() {
		return ciudad;
	}

	/**
	 * Asigna la ciudad donde queda; es el filtro principal de la búsqueda.
	 *
	 * @param ciudad ciudad donde queda; es el filtro principal de la búsqueda
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
	 * Devuelve el tipo de alojamiento, por ejemplo Casa o Apartamento.
	 *
	 * @return tipo de alojamiento, por ejemplo Casa o Apartamento
	 */
	public String getTipo() {
		return tipo;
	}

	/**
	 * Asigna el tipo de alojamiento, por ejemplo Casa o Apartamento.
	 *
	 * @param tipo tipo de alojamiento, por ejemplo Casa o Apartamento
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
	 * Devuelve la hora desde la que el huésped puede llegar (check-in).
	 *
	 * @return hora desde la que el huésped puede llegar (check-in)
	 */
	public LocalTime getHoraEntrada() {
		return horaEntrada;
	}

	/**
	 * Asigna la hora desde la que el huésped puede llegar (check-in).
	 *
	 * @param horaEntrada hora desde la que el huésped puede llegar (check-in)
	 */
	public void setHoraEntrada(LocalTime horaEntrada) {
		this.horaEntrada = horaEntrada;
	}

	/**
	 * Devuelve la hora límite de salida del huésped (check-out).
	 *
	 * @return hora límite de salida del huésped (check-out)
	 */
	public LocalTime getHoraSalida() {
		return horaSalida;
	}

	/**
	 * Asigna la hora límite de salida del huésped (check-out).
	 *
	 * @param horaSalida hora límite de salida del huésped (check-out)
	 */
	public void setHoraSalida(LocalTime horaSalida) {
		this.horaSalida = horaSalida;
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
	 * Devuelve el porcentaje propio de limpieza; si es null se usa el global de ConfiguracionTarifa.
	 *
	 * @return porcentaje propio de limpieza; si es null se usa el global de ConfiguracionTarifa
	 */
	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	/**
	 * Asigna el porcentaje propio de limpieza; si es null se usa el global de ConfiguracionTarifa.
	 *
	 * @param porcentajeLimpieza porcentaje propio de limpieza; si es null se usa el global de ConfiguracionTarifa
	 */
	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	/**
	 * Devuelve el porcentaje propio de servicio; si es null se usa el global de ConfiguracionTarifa.
	 *
	 * @return porcentaje propio de servicio; si es null se usa el global de ConfiguracionTarifa
	 */
	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	/**
	 * Asigna el porcentaje propio de servicio; si es null se usa el global de ConfiguracionTarifa.
	 *
	 * @param porcentajeServicio porcentaje propio de servicio; si es null se usa el global de ConfiguracionTarifa
	 */
	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	/**
	 * Devuelve las reglas de la casa, por ejemplo "No fumar".
	 *
	 * @return reglas de la casa, por ejemplo "No fumar"
	 */
	public String getReglas() {
		return reglas;
	}

	/**
	 * Asigna las reglas de la casa, por ejemplo "No fumar".
	 *
	 * @param reglas reglas de la casa, por ejemplo "No fumar"
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
	 * Devuelve el anfitrión dueño del alojamiento.
	 *
	 * @return anfitrión dueño del alojamiento
	 */
	public Long getIdAnfitrion() {
		return idAnfitrion;
	}

	/**
	 * Asigna el anfitrión dueño del alojamiento.
	 *
	 * @param idAnfitrion anfitrión dueño del alojamiento
	 */
	public void setIdAnfitrion(Long idAnfitrion) {
		this.idAnfitrion = idAnfitrion;
	}

	/**
	 * Devuelve la política de cancelación que eligió el anfitrión.
	 *
	 * @return política de cancelación que eligió el anfitrión
	 */
	public Long getIdPolitica() {
		return idPolitica;
	}

	/**
	 * Asigna la política de cancelación que eligió el anfitrión.
	 *
	 * @param idPolitica política de cancelación que eligió el anfitrión
	 */
	public void setIdPolitica(Long idPolitica) {
		this.idPolitica = idPolitica;
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
	 * Devuelve la última edición de los datos o de las imágenes de la publicación.
	 *
	 * @return última edición de los datos o de las imágenes de la publicación
	 */
	public LocalDateTime getFechaActualizacion() {
		return fechaActualizacion;
	}

	/**
	 * Asigna la última edición de los datos o de las imágenes de la publicación.
	 *
	 * @param fechaActualizacion última edición de los datos o de las imágenes de la publicación
	 */
	public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
		this.fechaActualizacion = fechaActualizacion;
	}

	/**
	 * Devuelve las amenidades del alojamiento (wifi, piscina, cocina...).
	 *
	 * @return amenidades del alojamiento (wifi, piscina, cocina...)
	 */
	public Set<String> getAmenidades() {
		return amenidades;
	}

	/**
	 * Asigna las amenidades del alojamiento (wifi, piscina, cocina...).
	 *
	 * @param amenidades amenidades del alojamiento (wifi, piscina, cocina...)
	 */
	public void setAmenidades(Set<String> amenidades) {
		this.amenidades = amenidades;
	}
}
