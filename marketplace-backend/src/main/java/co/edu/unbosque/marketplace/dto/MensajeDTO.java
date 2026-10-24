package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Mensaje de una conversación.
 *
 * @see "RF-029, CA-168"
 */
public class MensajeDTO {
    private Long idMensaje;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private Long idConversacion;
    private Long idUsuario;

    public MensajeDTO() {
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
