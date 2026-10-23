package co.edu.unbosque.marketplace.dto;

import co.edu.unbosque.marketplace.entity.EstadoResena;

public class ResenaDTO {
    private Long idResena;
    private Integer calificacion;
    private String comentario;
    private EstadoResena estado;
    private Long idUsuario;
    private Long idReserva;
    private Long idAlojamiento;

    public ResenaDTO() {
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
