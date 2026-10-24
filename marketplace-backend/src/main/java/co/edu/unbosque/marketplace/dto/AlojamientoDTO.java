package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

/**
 * Datos de un alojamiento. De entrada se ignoran id, estado, idAnfitrion y fechaCreacion: los asigna el service.
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
    private List<String> imagenes;
    private List<ServicioAdicionalDTO> serviciosAdicionales;

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
}
