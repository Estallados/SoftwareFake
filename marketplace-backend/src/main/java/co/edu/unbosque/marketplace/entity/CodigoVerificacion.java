package co.edu.unbosque.marketplace.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa un código de verificación de correo. Se envía al
 * registrarse y se guarda solo su hash; el código en claro únicamente viaja en
 * el correo al usuario. Vence, sirve una sola vez y admite pocos intentos.
 *
 * @see "RF-001"
 */
@Entity
@Table(name = "codigoVerificacion")
public class CodigoVerificacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idCodigo;
    @Column(nullable = false)
    private Long idUsuario;
    @Column(nullable = false)
    private String codigoHash;
    @Column(nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;
    /** Intentos fallidos con este código; al llegar al máximo deja de servir. */
    @Column(nullable = false)
    private Integer intentos;
    @Column(nullable = false)
    private Boolean usado;

    public CodigoVerificacion() {
    }

    public Long getIdCodigo() {
        return idCodigo;
    }
    public void setIdCodigo(Long idCodigo) {
        this.idCodigo = idCodigo;
    }

    public Long getIdUsuario() {
        return idUsuario;
    }
    public void setIdUsuario(Long idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getCodigoHash() {
        return codigoHash;
    }
    public void setCodigoHash(String codigoHash) {
        this.codigoHash = codigoHash;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }
    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }

    public Integer getIntentos() {
        return intentos;
    }
    public void setIntentos(Integer intentos) {
        this.intentos = intentos;
    }

    public Boolean getUsado() {
        return usado;
    }
    public void setUsado(Boolean usado) {
        this.usado = usado;
    }
}
