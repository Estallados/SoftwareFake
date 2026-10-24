package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoCancelacion;
import co.edu.unbosque.marketplace.entity.EstadoReserva;

/**
 * Registro de una cancelación.
 *
 * @see "RF-002"
 */
public class CancelacionDTO {
    private Long idCancelacion;
    private String motivo;
    private String politica;
    private Integer porcentajeReembolso;
    private BigDecimal valorReembolso;
    private EstadoCancelacion estado;
    private EstadoReserva estadoFinalReserva;
    private LocalDateTime fecha;
    private Long idUsuario;
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

    public String getPolitica() {
        return politica;
    }
    public void setPolitica(String politica) {
        this.politica = politica;
    }

    public Integer getPorcentajeReembolso() {
        return porcentajeReembolso;
    }
    public void setPorcentajeReembolso(Integer porcentajeReembolso) {
        this.porcentajeReembolso = porcentajeReembolso;
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

    public EstadoReserva getEstadoFinalReserva() {
        return estadoFinalReserva;
    }
    public void setEstadoFinalReserva(EstadoReserva estadoFinalReserva) {
        this.estadoFinalReserva = estadoFinalReserva;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdReserva() {
        return idReserva;
    }
    public void setIdReserva(Long idReserva) {
        this.idReserva = idReserva;
    }
}
