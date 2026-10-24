package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Entidad que representa favorito en el marketplace. Un alojamiento aparece una
 * sola vez en los favoritos de cada usuario.
 *
 * @see "RF-007, CA-172"
 */
@Entity
@Table(name = "favorito", uniqueConstraints = @UniqueConstraint(name = "uk_favorito_usuario_alojamiento",
        columnNames = { "id_usuario", "id_alojamiento" }))
public class Favorito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFavorito;
    private LocalDateTime fechaAgregado;
    private Long idUsuario;
    private Long idAlojamiento;

    public Favorito() {
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
}
