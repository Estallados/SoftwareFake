package co.edu.unbosque.marketplace.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Política de cancelación que el anfitrión elige para su alojamiento. Define
 * cuánto se le devuelve al huésped según los días de anticipación con que
 * cancela:
 * <ul>
 * <li>100 % si faltan al menos {@code diasReembolsoTotal} días;</li>
 * <li>{@code porcentajeParcial} % si faltan al menos
 * {@code diasReembolsoParcial} días;</li>
 * <li>0 % en otro caso.</li>
 * </ul>
 * Un valor {@code null} en los días significa que ese tramo no existe.
 */
@Entity
@Table(name = "politicaCancelacion")
public class PoliticaCancelacion {

	/** Identificador de la política. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idPolitica;

	/** Nombre de la política: FLEXIBLE, MODERADA o ESTRICTA. */
	private String nombre;

	/** Explicación para el huésped de cuánto se devuelve y cuándo. */
	private String condiciones;

	/** Días mínimos de anticipación para devolver el 100 %; {@code null} si no aplica. */
	private Integer diasReembolsoTotal;

	/** Días mínimos de anticipación para la devolución parcial; {@code null} si no aplica. */
	private Integer diasReembolsoParcial;

	/** Porcentaje que se devuelve en el tramo parcial. */
	private Integer porcentajeParcial;


	/** Constructor vacío, requerido por JPA. */
	public PoliticaCancelacion() {
	}

	/**
	 * Devuelve el identificador de la política.
	 *
	 * @return identificador de la política
	 */
	public Long getIdPolitica() {
		return idPolitica;
	}

	/**
	 * Asigna el identificador de la política.
	 *
	 * @param idPolitica identificador de la política
	 */
	public void setIdPolitica(Long idPolitica) {
		this.idPolitica = idPolitica;
	}

	/**
	 * Devuelve el nombre de la política: FLEXIBLE, MODERADA o ESTRICTA.
	 *
	 * @return nombre de la política: FLEXIBLE, MODERADA o ESTRICTA
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre de la política: FLEXIBLE, MODERADA o ESTRICTA.
	 *
	 * @param nombre nombre de la política: FLEXIBLE, MODERADA o ESTRICTA
	 */
	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	/**
	 * Devuelve la explicación para el huésped de cuánto se devuelve y cuándo.
	 *
	 * @return explicación para el huésped de cuánto se devuelve y cuándo
	 */
	public String getCondiciones() {
		return condiciones;
	}

	/**
	 * Asigna la explicación para el huésped de cuánto se devuelve y cuándo.
	 *
	 * @param condiciones explicación para el huésped de cuánto se devuelve y cuándo
	 */
	public void setCondiciones(String condiciones) {
		this.condiciones = condiciones;
	}

	/**
	 * Devuelve los días mínimos de anticipación para devolver el 100 %; {@code null} si no aplica.
	 *
	 * @return días mínimos de anticipación para devolver el 100 %; {@code null} si no aplica
	 */
	public Integer getDiasReembolsoTotal() {
		return diasReembolsoTotal;
	}

	/**
	 * Asigna los días mínimos de anticipación para devolver el 100 %; {@code null} si no aplica.
	 *
	 * @param diasReembolsoTotal días mínimos de anticipación para devolver el 100 %; {@code null} si no aplica
	 */
	public void setDiasReembolsoTotal(Integer diasReembolsoTotal) {
		this.diasReembolsoTotal = diasReembolsoTotal;
	}

	/**
	 * Devuelve los días mínimos de anticipación para la devolución parcial; {@code null} si no aplica.
	 *
	 * @return días mínimos de anticipación para la devolución parcial; {@code null} si no aplica
	 */
	public Integer getDiasReembolsoParcial() {
		return diasReembolsoParcial;
	}

	/**
	 * Asigna los días mínimos de anticipación para la devolución parcial; {@code null} si no aplica.
	 *
	 * @param diasReembolsoParcial días mínimos de anticipación para la devolución parcial; {@code null} si no aplica
	 */
	public void setDiasReembolsoParcial(Integer diasReembolsoParcial) {
		this.diasReembolsoParcial = diasReembolsoParcial;
	}

	/**
	 * Devuelve el porcentaje que se devuelve en el tramo parcial.
	 *
	 * @return porcentaje que se devuelve en el tramo parcial
	 */
	public Integer getPorcentajeParcial() {
		return porcentajeParcial;
	}

	/**
	 * Asigna el porcentaje que se devuelve en el tramo parcial.
	 *
	 * @param porcentajeParcial porcentaje que se devuelve en el tramo parcial
	 */
	public void setPorcentajeParcial(Integer porcentajeParcial) {
		this.porcentajeParcial = porcentajeParcial;
	}
}
