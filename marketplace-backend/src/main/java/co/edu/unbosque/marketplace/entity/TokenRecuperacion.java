package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa un token de recuperación de contraseña. Se guarda solo
 * el hash del token; el token en claro únicamente se entrega al usuario (en esta
 * versión, simulado en el log).
 *
 * @see "Módulo 1: identidad y perfil (RF-008 a RF-012)"
 */
@Entity
@Table(name = "tokenRecuperacion")
public class TokenRecuperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idToken;
    @Column(nullable = false)
    private Long idUsuario;
    @Column(nullable = false, unique = true)
    private String tokenHash;
    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;
    @Column(nullable = false)
    private Boolean usado;

    public TokenRecuperacion() {
    }

    public Long getIdToken() {
        return idToken;
    }
    public void setIdToken(Long idToken) {
        this.idToken = idToken;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getTokenHash() {
        return tokenHash;
    }
    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public Boolean getUsado() {
        return usado;
    }
    public void setUsado(Boolean usado) {
        this.usado = usado;
    }
}
