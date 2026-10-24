package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa un evento de auditoría en el marketplace.
 * <p>
 * Es de solo inserción: {@code @Immutable} hace que Hibernate ignore cualquier
 * actualización, y el repositorio no expone update ni delete.
 * </p>
 *
 * @see "RNF-023, RNF-026"
 */
@Entity
@Immutable
@Table(name = "eventoAuditoria")
public class EventoAuditoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idEvento;
    @Column(nullable = false, updatable = false)
    private String accion;
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;
    @Column(length = 1000, updatable = false)
    private String detalle;
    /** Usuario que hizo la acción; null si no se conoce (por ejemplo, un login fallido). */
    @Column(updatable = false)
    private Long idUsuario;
    /** Tipo de recurso afectado, por ejemplo USUARIO o ALOJAMIENTO. */
    @Column(updatable = false)
    private String entidad;
    @Column(updatable = false)
    private Long idEntidad;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ResultadoAuditoria resultado;

    public EventoAuditoria() {
    }

    public Long getIdEvento() {
        return idEvento;
    }
    public void setIdEvento(Long idEvento) {
        this.idEvento = idEvento;
    }

    public String getAccion() {
        return accion;
    }
    public void setAccion(String accion) {
        this.accion = accion;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getDetalle() {
        return detalle;
    }
    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getEntidad() {
        return entidad;
    }
    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public Long getIdEntidad() {
        return idEntidad;
    }
    public void setIdEntidad(Long idEntidad) {
        this.idEntidad = idEntidad;
    }

    public ResultadoAuditoria getResultado() {
        return resultado;
    }
    public void setResultado(ResultadoAuditoria resultado) {
        this.resultado = resultado;
    }
}
