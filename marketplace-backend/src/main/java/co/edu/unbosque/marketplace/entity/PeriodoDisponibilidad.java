package co.edu.unbosque.marketplace.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que representa un período en que el anfitrión ofrece su alojamiento.
 * Una estancia [llegada, salida) cabe en el período si llegada &gt;= fechaInicio
 * y salida &lt;= fechaFin. Las fechas reservadas no se guardan aquí: se derivan
 * de las reservas activas (RF-020).
 *
 * @see "RF-025, RF-026"
 */
@Entity
@Table(name = "periodoDisponibilidad")
public class PeriodoDisponibilidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPeriodo;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    public PeriodoDisponibilidad() {
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
