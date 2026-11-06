package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

/**
 * Servicio opcional que el anfitrión ofrece con su alojamiento (desayuno,
 * traslado desde el aeropuerto, etc.). El huésped puede agregarlo a su
 * reserva y su costo se suma una vez al total.
 */
@Entity
@Table(name = "servicioAdicional")
public class ServicioAdicional {

    /** Identificador del servicio. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idServicio;

    /** Nombre que ve el huésped, por ejemplo "Desayuno". */
    private String nombre;

    /** Costo del servicio por reserva, en pesos colombianos. */
    private BigDecimal costo;

    /** Alojamiento que ofrece el servicio. */
    @Column(name = "id_alojamiento")
    private Long idAlojamiento;

    /** Clave foránea hacia {@link Alojamiento}; el valor se asigna con {@code idAlojamiento}. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
    private Alojamiento alojamiento;

    /** Constructor vacío, requerido por JPA. */
    public ServicioAdicional() {
    }

    /**
     * Devuelve el identificador del servicio.
     *
     * @return identificador del servicio
     */
    public Long getIdServicio() {
        return idServicio;
    }
    /**
     * Asigna el identificador del servicio.
     *
     * @param idServicio identificador del servicio
     */
    public void setIdServicio(Long idServicio) {
        this.idServicio = idServicio;
    }

    /**
     * Devuelve el nombre que ve el huésped, por ejemplo "Desayuno".
     *
     * @return nombre que ve el huésped, por ejemplo "Desayuno"
     */
    public String getNombre() {
        return nombre;
    }

	/**
	 * Asigna el nombre que ve el huésped, por ejemplo "Desayuno".
	 *
	 * @param nombre nombre que ve el huésped, por ejemplo "Desayuno"
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

    /**
     * Devuelve el costo del servicio por reserva, en pesos colombianos.
     *
     * @return costo del servicio por reserva, en pesos colombianos
     */
    public BigDecimal getCosto() {
        return costo;
    }
    /**
     * Asigna el costo del servicio por reserva, en pesos colombianos.
     *
     * @param costo costo del servicio por reserva, en pesos colombianos
     */
    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    /**
     * Devuelve el alojamiento que ofrece el servicio.
     *
     * @return alojamiento que ofrece el servicio
     */
    public Long getIdAlojamiento() {
        return idAlojamiento;
    }
    /**
     * Asigna el alojamiento que ofrece el servicio.
     *
     * @param idAlojamiento alojamiento que ofrece el servicio
     */
    public void setIdAlojamiento(Long idAlojamiento) {
        this.idAlojamiento = idAlojamiento;
    }
}
