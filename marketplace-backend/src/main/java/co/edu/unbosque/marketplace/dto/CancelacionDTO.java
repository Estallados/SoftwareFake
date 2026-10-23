package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;

import co.edu.unbosque.marketplace.entity.EstadoCancelacion;

public class CancelacionDTO {
    private Long idCancelacion;
    private String motivo;
    private BigDecimal valorReembolso;
    private EstadoCancelacion estado;
    private Long idReserva;

    public CancelacionDTO() {
    }

    public Long getIdCancelacion() {
        return idCancelacion;
    }
    public void setIdCancelacion(Long idCancelacion) {
        this.idCancelacion = idCancelacion;
    }

    public String getMotivo() {
        return motivo;
    }
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public BigDecimal getValorReembolso() {
        return valorReembolso;
    }
    public void setValorReembolso(BigDecimal valorReembolso) {
        this.valorReembolso = valorReembolso;
    }

    public EstadoCancelacion getEstado() {
        return estado;
    }
    public void setEstado(EstadoCancelacion estado) {
        this.estado = estado;
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }
}
