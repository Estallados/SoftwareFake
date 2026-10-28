package co.edu.unbosque.marketplace.entity;

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
 * Entidad que representa conversacion entre un huésped ({@code idUsuario}) y el
 * anfitrión ({@code idAnfitrion}) de un alojamiento.
 *
 * @see "RF-029"
 */
@Entity
@Table(name = "conversacion")
public class Conversacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConversacion;
    private LocalDateTime fechaCreacion;
    @Enumerated(EnumType.STRING)
    private EstadoConversacion estado;
    @Column(name = "id_usuario")
    private Long idUsuario;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idUsuario}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario huesped;
    @Column(name = "id_anfitrion")
    private Long idAnfitrion;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idAnfitrion}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_anfitrion", insertable = false, updatable = false)
    private Usuario anfitrion;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;

    public Conversacion() {
    }

    public Long getIdConversacion() {
        return idConversacion;
    }
    public void setIdConversacion(Long idConversacion) {
        this.idConversacion = idConversacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public EstadoConversacion getEstado() {
        return estado;
    }
    public void setEstado(EstadoConversacion estado) {
        this.estado = estado;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Long getIdAnfitrion() {
        return idAnfitrion;
    }
    public void setIdAnfitrion(Long idAnfitrion) {
        this.idAnfitrion = idAnfitrion;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
