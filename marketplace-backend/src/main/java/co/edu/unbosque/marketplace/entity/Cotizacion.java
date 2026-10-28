package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que representa cotizacion en el marketplace. Guarda cada componente
 * del precio y los porcentajes vigentes al momento de cotizar; una reserva
 * solo puede crearse desde una cotización vigente y sin usar.
 *
 * @see "RF-021, RF-023, RNF-031"
 */
@Entity
@Table(name = "cotizacion")
public class Cotizacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCotizacion;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer huespedes;
    private Integer noches;
    private BigDecimal precioNoche;
    private BigDecimal precioBase;
    private BigDecimal descuento;
    private BigDecimal porcentajeLimpieza;
    private BigDecimal valorLimpieza;
    private BigDecimal porcentajeServicio;
    private BigDecimal valorServicio;
    private BigDecimal valorAdicionales;
    private BigDecimal total;
    /** Servicios adicionales elegidos, en JSON con nombre y costo congelados. */
    private String desglose;
    @Column(name = "id_usuario")
    private Long idUsuario;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idUsuario}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;
    @Column(name = "id_reserva")
    private Long idReserva;
    /** Clave foránea hacia {@link Reserva}; el valor se asigna con {@code idReserva}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", insertable = false, updatable = false)
    private Reserva reserva;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaExpiracion;

    public Cotizacion() {
    }

    public Long getIdCotizacion() {
        return idCotizacion;
    }
    public void setIdCotizacion(Long idCotizacion) {
        this.idCotizacion = idCotizacion;
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

    public Integer getHuespedes() {
        return huespedes;
    }
    public void setHuespedes(Integer huespedes) {
        this.huespedes = huespedes;
    }

    public Integer getNoches() {
        return noches;
    }
    public void setNoches(Integer noches) {
        this.noches = noches;
    }

    public BigDecimal getPrecioNoche() {
        return precioNoche;
    }
    public void setPrecioNoche(BigDecimal precioNoche) {
        this.precioNoche = precioNoche;
    }

    public BigDecimal getPrecioBase() {
        return precioBase;
    }
    public void setPrecioBase(BigDecimal precioBase) {
        this.precioBase = precioBase;
    }

    public BigDecimal getDescuento() {
        return descuento;
    }
    public void setDescuento(BigDecimal descuento) {
        this.descuento = descuento;
    }

    public BigDecimal getPorcentajeLimpieza() {
        return porcentajeLimpieza;
    }
    public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
        this.porcentajeLimpieza = porcentajeLimpieza;
    }

    public BigDecimal getValorLimpieza() {
        return valorLimpieza;
    }
    public void setValorLimpieza(BigDecimal valorLimpieza) {
        this.valorLimpieza = valorLimpieza;
    }

    public BigDecimal getPorcentajeServicio() {
        return porcentajeServicio;
    }
    public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
        this.porcentajeServicio = porcentajeServicio;
    }

    public BigDecimal getValorServicio() {
        return valorServicio;
    }
    public void setValorServicio(BigDecimal valorServicio) {
        this.valorServicio = valorServicio;
    }

    public BigDecimal getValorAdicionales() {
        return valorAdicionales;
    }
    public void setValorAdicionales(BigDecimal valorAdicionales) {
        this.valorAdicionales = valorAdicionales;
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

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
