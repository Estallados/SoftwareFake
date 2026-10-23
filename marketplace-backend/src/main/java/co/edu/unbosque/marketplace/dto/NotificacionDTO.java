package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

public class NotificacionDTO {
    private Long idNotificacion;
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
