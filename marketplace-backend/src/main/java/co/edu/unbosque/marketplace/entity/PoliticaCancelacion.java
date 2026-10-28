package co.edu.unbosque.marketplace.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa politicacancelacion en el marketplace. Define la
 * devolución según los días de anticipación con que se cancela:
 * <ul>
 * <li>100 % si faltan al menos {@code diasReembolsoTotal} días;</li>
 * <li>{@code porcentajeParcial} % si faltan al menos {@code diasReembolsoParcial} días;</li>
 * <li>0 % en otro caso.</li>
 * </ul>
 * Un valor null en los días significa que ese tramo no existe.
 *
 * @see "RF-002"
 */
@Entity
@Table(name = "politicaCancelacion")
public class PoliticaCancelacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPolitica;
    private String nombre;
    private String condiciones;
    private Integer diasReembolsoTotal;
    private Integer diasReembolsoParcial;
    private Integer porcentajeParcial;

    public PoliticaCancelacion() {
    }

    public Long getIdPolitica() {
        return idPolitica;
    }
    public void setIdPolitica(Long idPolitica) {
        this.idPolitica = idPolitica;
    }

    public String getNombre() {
        return nombre;
    }
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCondiciones() {
        return condiciones;
    }
    public void setCondiciones(String condiciones) {
        this.condiciones = condiciones;
    }

    public Integer getDiasReembolsoTotal() {
        return diasReembolsoTotal;
    }
    public void setDiasReembolsoTotal(Integer diasReembolsoTotal) {
        this.diasReembolsoTotal = diasReembolsoTotal;
    }

    public Integer getDiasReembolsoParcial() {
        return diasReembolsoParcial;
    }
    public void setDiasReembolsoParcial(Integer diasReembolsoParcial) {
        this.diasReembolsoParcial = diasReembolsoParcial;
    }

    public Integer getPorcentajeParcial() {
        return porcentajeParcial;
    }
    public void setPorcentajeParcial(Integer porcentajeParcial) {
        this.porcentajeParcial = porcentajeParcial;
    }
}
