package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa cancelacion en el marketplace: quién canceló, cuándo,
 * por qué, la política aplicada, la devolución y el estado final de la reserva.
 *
 * @see "RF-002, CA-004, CA-005, CA-006"
 */
@Entity
@Table(name = "cancelacion")
public class Cancelacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCancelacion;
    @Column(length = 1000)
    private String motivo;
    /** Nombre de la política aplicada (FLEXIBLE, MODERADA, ESTRICTA). */
    private String politica;
    private Integer porcentajeReembolso;
    private BigDecimal valorReembolso;
    @Enumerated(EnumType.STRING)
    private EstadoCancelacion estado;
    @Enumerated(EnumType.STRING)
    private EstadoReserva estadoFinalReserva;
    private LocalDateTime fecha;
    private Long idUsuario;
    private Long idReserva;

    public Cancelacion() {
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
