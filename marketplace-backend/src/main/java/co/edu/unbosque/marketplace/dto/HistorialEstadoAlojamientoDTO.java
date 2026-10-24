package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.EstadoAlojamiento;

/**
 * Un cambio de estado de un alojamiento.
 *
 * @see "RF-027, CA-153"
 */
public class HistorialEstadoAlojamientoDTO {
    private Long idHistorial;
    private Long idAlojamiento;
    private EstadoAlojamiento estadoAnterior;
    private EstadoAlojamiento estadoNuevo;
    private String motivo;
    private Long idUsuario;
    private LocalDateTime fecha;

    public HistorialEstadoAlojamientoDTO() {
    }

    public Long getIdHistorial() {
        return idHistorial;
    }
    public void setIdHistorial(Long idHistorial) {
        this.idHistorial = idHistorial;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }

    public EstadoAlojamiento getEstadoAnterior() {
        return estadoAnterior;
    }
    public void setEstadoAnterior(EstadoAlojamiento estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoAlojamiento getEstadoNuevo() {
        return estadoNuevo;
    }
    public void setEstadoNuevo(EstadoAlojamiento estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMotivo() {
        return motivo;
    }
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
