package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

import co.edu.unbosque.marketplace.entity.EstadoConversacion;

public class ConversacionDTO {
    private Long idConversacion;
    private LocalDateTime fechaCreacion;
    private EstadoConversacion estado;
    private Long idUsuario;
    private Long idAlojamiento;

    public ConversacionDTO() {
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

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
