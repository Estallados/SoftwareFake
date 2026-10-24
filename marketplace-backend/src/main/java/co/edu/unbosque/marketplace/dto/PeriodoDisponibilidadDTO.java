package co.edu.unbosque.marketplace.dto;

import java.time.LocalDate;

/**
 * Período disponible de un alojamiento: [fechaInicio, fechaFin].
 *
 * @see "RF-025"
 */
public class PeriodoDisponibilidadDTO {
    private Long idPeriodo;
    private Long idAlojamiento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public PeriodoDisponibilidadDTO() {
    }

    public Long getIdPeriodo() {
        return idPeriodo;
    }
    public void setIdPeriodo(Long idPeriodo) {
        this.idPeriodo = idPeriodo;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }
    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }
    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }
}
