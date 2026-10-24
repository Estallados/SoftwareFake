package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Alojamiento guardado en favoritos.
 *
 * @see "RF-007"
 */
public class FavoritoDTO {
    private Long idFavorito;
    private LocalDateTime fechaAgregado;
    private Long idUsuario;
    private Long idAlojamiento;
    private AlojamientoDTO alojamiento;

    public FavoritoDTO() {
    }

    public Long getIdFavorito() {
        return idFavorito;
    }
    public void setIdFavorito(Long idFavorito) {
        this.idFavorito = idFavorito;
    }

    public LocalDateTime getFechaAgregado() {
        return fechaAgregado;
    }
    public void setFechaAgregado(LocalDateTime fechaAgregado) {
        this.fechaAgregado = fechaAgregado;
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

    public AlojamientoDTO getAlojamiento() {
        return alojamiento;
    }
    public void setAlojamiento(AlojamientoDTO alojamiento) {
        this.alojamiento = alojamiento;
    }
}
