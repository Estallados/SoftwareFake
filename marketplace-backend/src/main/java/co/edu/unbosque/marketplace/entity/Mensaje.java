package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad que representa mensaje de una conversación: remitente
 * ({@code idUsuario}), contenido y fecha de envío.
 *
 * @see "RF-029, CA-168"
 */
@Entity
@Table(name = "mensaje")
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMensaje;
    private String contenido;
    private LocalDateTime fechaEnvio;
    @Column(name = "id_conversacion")
    private Long idConversacion;
    /** Clave foránea hacia {@link Conversacion}; el valor se asigna con {@code idConversacion}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conversacion", insertable = false, updatable = false)
    private Conversacion conversacion;
    @Column(name = "id_usuario")
    private Long idUsuario;
    /** Clave foránea hacia {@link Usuario}; el valor se asigna con {@code idUsuario}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", insertable = false, updatable = false)
    private Usuario usuario;

    public Mensaje() {
    }

    public Long getIdMensaje() {
        return idMensaje;
    }
    public void setIdMensaje(Long idMensaje) {
        this.idMensaje = idMensaje;
    }

    public String getContenido() {
        return contenido;
    }
    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }
    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public Long getIdConversacion() {
        return idConversacion;
    }
    public void setIdConversacion(Long idConversacion) {
        this.idConversacion = idConversacion;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
}
