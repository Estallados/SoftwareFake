package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Notificación dentro de la plataforma.
 *
 * @see "RF-003"
 */
public class NotificacionDTO {
    private Long idNotificacion;
    private String tipo;
    private String mensaje;
    private LocalDateTime fecha;
    private Boolean leida;
    private Long idUsuario;

    public NotificacionDTO() {
    }

    public Long getIdNotificacion() {
        return idNotificacion;
    }
    public void setIdNotificacion(Long idNotificacion) {
        this.idNotificacion = idNotificacion;
    }

    public String getTipo() {
        return tipo;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public Boolean getLeida() {
        return leida;
    }
    public void setLeida(Boolean leida) {
        this.leida = leida;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }
}
