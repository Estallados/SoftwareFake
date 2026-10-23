package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

public class ServicioAdicionalDTO {
    private Long idServicio;
    private String nombre;
    private BigDecimal costo;
    private Long idAlojamiento;

    public ServicioAdicionalDTO() {
    }

    public Long getIdServicio() {
        return idServicio;
    }
    public void setIdServicio(Long idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getCosto() {
        return costo;
    }
    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
