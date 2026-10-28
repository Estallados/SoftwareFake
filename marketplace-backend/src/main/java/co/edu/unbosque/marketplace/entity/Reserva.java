package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
 * Entidad que representa reserva en el marketplace. El desglose de precios se
 * congela al crearla (copia de la cotización), así los cambios posteriores de
 * tarifas o servicios adicionales no la alteran.
 *
 * @see "RF-020, RF-021, RF-022, RNF-031"
 */
@Entity
@Table(name = "reserva")
public class Reserva {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReserva;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer noches;
    private Integer huespedes;
    @Enumerated(EnumType.STRING)
    private EstadoReserva estado;
    private BigDecimal total;
    /** Desglose congelado en JSON (copia de la cotización usada). */
    private String desglose;
    @Column(name = "id_huesped")
    private Long idHuesped;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idHuesped}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_huesped", insertable = false, updatable = false)
    private Usuario huesped;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;
    @Column(name = "id_cotizacion")
    private Long idCotizacion;
    /** Clave foránea hacia {@link Cotizacion}; el valor se asigna con {@code idCotizacion}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion", insertable = false, updatable = false)
    private Cotizacion cotizacion;
    private LocalDateTime fechaCreacion;

    public Reserva() {
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public LocalDate getFechaEntrada() {
        return fechaEntrada;
    }
    public void setFechaEntrada(LocalDate fechaEntrada) {
        this.fechaEntrada = fechaEntrada;
    }

    public LocalDate getFechaSalida() {
        return fechaSalida;
    }
    public void setFechaSalida(LocalDate fechaSalida) {
        this.fechaSalida = fechaSalida;
    }

    public Integer getNoches() {
        return noches;
    }
    public void setNoches(Integer noches) {
        this.noches = noches;
    }

    public Integer getHuespedes() {
        return huespedes;
    }
    public void setHuespedes(Integer huespedes) {
        this.huespedes = huespedes;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }
    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getDesglose() {
        return desglose;
    }
    public void setDesglose(String desglose) {
        this.desglose = desglose;
    }

    public Long getIdHuesped() {
        return idHuesped;
    }
    public void setIdHuesped(Long idHuesped) {
        this.idHuesped = idHuesped;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public Long getIdCotizacion() {
        return idCotizacion;
    }
    public void setIdCotizacion(Long idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
