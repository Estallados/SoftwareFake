package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;


/** Entidad que representa cancelacion en el marketplace. */
@Entity
@Table(name = "cancelacion")
public class Cancelacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCancelacion;
    private String motivo;
    private BigDecimal valorReembolso;
    @Enumerated(EnumType.STRING)
    private EstadoCancelacion estado;
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
