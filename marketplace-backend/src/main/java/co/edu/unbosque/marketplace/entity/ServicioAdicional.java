package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

/** Entidad que representa servicioadicional en el marketplace. */
@Entity
@Table(name = "servicioAdicional")
public class ServicioAdicional {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicio;
    private String nombre;
    private BigDecimal costo;
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;
    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;

    public ServicioAdicional() {
    }

    public Long getIdServicio() {
        return idServicio;
    }
    public void setIdServicio(Long idServicio) {
        this.idServicio = idServicio;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getCosto() {
        return costo;
    }
    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
