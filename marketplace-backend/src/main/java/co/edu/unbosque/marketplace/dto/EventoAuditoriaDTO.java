package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;
import co.edu.unbosque.marketplace.entity.ResultadoAuditoria;

/**
 * Evento de auditoría para consulta del administrador.
 *
 * @see "RF-015, CA-064, CA-138"
 */
public class EventoAuditoriaDTO {
    private Long idEvento;
    private String accion;
    private LocalDateTime fecha;
    private String detalle;
    private Long idUsuario;
    private String entidad;
    private Long idEntidad;
    private ResultadoAuditoria resultado;

    public EventoAuditoriaDTO() {
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
