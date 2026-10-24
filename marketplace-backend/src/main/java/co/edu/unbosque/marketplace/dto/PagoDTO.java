package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoPago;

/**
 * Pago simulado de una reserva.
 *
 * @see "RF-028"
 */
public class PagoDTO {
    private Long idPago;
    private String referencia;
    private String proveedor;
    private BigDecimal valor;
    private EstadoPago estado;
    private String detalle;
    private LocalDateTime fecha;
    private Long idReserva;

    public PagoDTO() {
    }

    public Long getIdPago() {
        return idPago;
    }
    public void setIdPago(Long idPago) {
        this.idPago = idPago;
    }

    public String getReferencia() {
        return referencia;
    }
    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public String getProveedor() {
        return proveedor;
    }
    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public BigDecimal getValor() {
        return valor;
    }
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public EstadoPago getEstado() {
        return estado;
    }
    public void setEstado(EstadoPago estado) {
        this.estado = estado;
    }

    public String getDetalle() {
        return detalle;
    }
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }
}
