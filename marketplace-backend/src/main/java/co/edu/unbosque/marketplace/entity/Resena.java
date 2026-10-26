package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que representa resena en el marketplace. Hay como máximo una por
 * reserva. Ocultarla (moderación) no la borra.
 *
 * @see "RF-004, RF-016"
 */
@Entity
@Table(name = "resena", indexes = @Index(name = "idx_resena_alojamiento", columnList = "id_alojamiento"))
public class Resena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResena;
    private Integer calificacion;
    @Column(length = 2000)
    private String comentario;
    @Enumerated(EnumType.STRING)
    private EstadoResena estado;
    private LocalDateTime fecha;
    @Column(name = "id_usuario")
    private Long idUsuario;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idUsuario}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resena_usuario"))
    private Usuario usuario;
    @Column(name = "id_reserva", unique = true)
    private Long idReserva;
    /** Clave foránea hacia {@link Reserva}; el valor se asigna con {@code idReserva}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_reserva", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resena_reserva"))
    private Reserva reserva;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_resena_alojamiento"))
    private Alojamiento alojamiento;

    public Resena() {
    }

    public Long getIdResena() {
        return idResena;
    }
    public void setIdResena(Long idResena) {
        this.idResena = idResena;
    }

    public Integer getCalificacion() {
        return calificacion;
    }
    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }
    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public EstadoResena getEstado() {
        return estado;
    }
    public void setEstado(EstadoResena estado) {
        this.estado = estado;
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

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
