package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

public class AlojamientoDTO {
    private Long idAlojamiento;
    private String titulo;
    private String ubicacion;
    private String tipo;
    private Integer capacidad;
    private BigDecimal precioNoche;
    private EstadoAlojamiento estado;
    private Long idAnfitrion;

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

    public String getUbicacion() {
        return ubicacion;
    }
    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
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
}
