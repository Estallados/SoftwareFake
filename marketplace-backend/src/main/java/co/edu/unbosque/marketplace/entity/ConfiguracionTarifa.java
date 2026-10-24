package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que guarda los porcentajes globales de limpieza y servicio. Cada
 * cambio inserta una fila nueva; la vigente es la más reciente.
 *
 * @see "RF-023"
 */
@Entity
@Table(name = "configuracionTarifa")
public class ConfiguracionTarifa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConfiguracion;
    @Column(nullable = false)
    private BigDecimal porcentajeLimpieza;
    @Column(nullable = false)
    private BigDecimal porcentajeServicio;
    private Long idUsuario;
    @Column(nullable = false)
    private LocalDateTime fecha;

    public ConfiguracionTarifa() {
    }

    public Long getIdConfiguracion() {
        return idConfiguracion;
    }
    public void setIdConfiguracion(Long idConfiguracion) {
        this.idConfiguracion = idConfiguracion;
    }

    public BigDecimal getPorcentajeLimpieza() {
        return porcentajeLimpieza;
    }
    public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
        this.porcentajeLimpieza = porcentajeLimpieza;
    }

    public BigDecimal getPorcentajeServicio() {
        return porcentajeServicio;
    }
    public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
        this.porcentajeServicio = porcentajeServicio;
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
