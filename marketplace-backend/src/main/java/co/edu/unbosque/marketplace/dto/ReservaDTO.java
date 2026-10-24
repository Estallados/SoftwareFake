package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Datos de una reserva con su desglose congelado y el último pago.
 *
 * @see "RF-021, RF-022"
 */
public class ReservaDTO {
    private Long idReserva;
    private LocalDate fechaEntrada;
    private LocalDate fechaSalida;
    private Integer noches;
    private Integer huespedes;
    private EstadoReserva estado;
    private BigDecimal total;
    private String desglose;
    private Long idHuesped;
    private Long idAlojamiento;
    private String tituloAlojamiento;
    private Long idCotizacion;
    private LocalDateTime fechaCreacion;
    private Boolean completada;
    private PagoDTO pago;

    public ReservaDTO() {
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

    public String getTituloAlojamiento() {
        return tituloAlojamiento;
    }
    public void setTituloAlojamiento(String tituloAlojamiento) {
        this.tituloAlojamiento = tituloAlojamiento;
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

    public Boolean getCompletada() {
        return completada;
    }
    public void setCompletada(Boolean completada) {
        this.completada = completada;
    }

    public PagoDTO getPago() {
        return pago;
    }
    public void setPago(PagoDTO pago) {
        this.pago = pago;
    }
}
