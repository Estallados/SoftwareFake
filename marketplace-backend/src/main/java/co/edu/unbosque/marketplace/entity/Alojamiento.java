package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que representa alojamiento (la publicación del anfitrión) en el
 * marketplace. Las imágenes y los servicios adicionales están en sus propias
 * entidades ({@link Imagen}, {@link ServicioAdicional}); las amenidades
 * (servicios del alojamiento como wifi o piscina) son una colección de textos.
 * <p>
 * Tiene índices en las columnas que filtra la búsqueda (RNF-015).
 * </p>
 *
 * @see "RF-017, RF-018, RF-019, RF-023, RF-024, RNF-032"
 */
@Entity
@Table(name = "alojamiento", indexes = {
        @Index(name = "idx_alojamiento_estado", columnList = "estado"),
        @Index(name = "idx_alojamiento_ciudad", columnList = "ciudad"),
        @Index(name = "idx_alojamiento_tipo", columnList = "tipo"),
        @Index(name = "idx_alojamiento_precio", columnList = "precio_noche"),
        @Index(name = "idx_alojamiento_capacidad", columnList = "capacidad"),
        @Index(name = "idx_alojamiento_anfitrion", columnList = "id_anfitrion") })
public class Alojamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAlojamiento;
    private String titulo;
    @Column(length = 2000)
    private String descripcion;
    /** Dirección o referencia textual de la ubicación. */
    private String ubicacion;
    private String ciudad;
    /** Coordenadas opcionales; el front las usa para el mapa (RNF-032). */
    private Double latitud;
    private Double longitud;
    private String tipo;
    private Integer capacidad;
    private Integer habitaciones;
    private Integer banos;
    /** Hora desde la que el huésped puede llegar (check-in). */
    private LocalTime horaEntrada;
    /** Hora límite de salida del huésped (check-out). */
    private LocalTime horaSalida;
    private BigDecimal precioNoche;
    /** Porcentaje propio de limpieza; si es null se usa el global de ConfiguracionTarifa. */
    private BigDecimal porcentajeLimpieza;
    /** Porcentaje propio de servicio; si es null se usa el global de ConfiguracionTarifa. */
    private BigDecimal porcentajeServicio;
    @Column(length = 2000)
    private String reglas;
    @Enumerated(EnumType.STRING)
    private EstadoAlojamiento estado;
    @Column(name = "id_anfitrion")
    private Long idAnfitrion;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idAnfitrion}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_anfitrion", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_alojamiento_anfitrion"))
    private Usuario anfitrion;
    @Column(name = "id_politica")
    private Long idPolitica;
    /** Clave foránea hacia {@link PoliticaCancelacion}; el valor se asigna con {@code idPolitica}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_politica", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_alojamiento_politica"))
    private PoliticaCancelacion politica;
    private LocalDateTime fechaCreacion;
    /** Última edición de los datos o de las imágenes de la publicación. */
    private LocalDateTime fechaActualizacion;
    /** Clave primaria (id_alojamiento, amenidad): un alojamiento no repite amenidades. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alojamientoAmenidad", joinColumns = @JoinColumn(name = "id_alojamiento"),
            foreignKey = @ForeignKey(name = "fk_alojamiento_amenidad_alojamiento"))
    @Column(name = "amenidad", nullable = false)
    private Set<String> amenidades = new HashSet<>();

    public Alojamiento() {
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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public Set<String> getAmenidades() {
        return amenidades;
    }
    public void setAmenidades(Set<String> amenidades) {
        this.amenidades = amenidades;
    }
}
