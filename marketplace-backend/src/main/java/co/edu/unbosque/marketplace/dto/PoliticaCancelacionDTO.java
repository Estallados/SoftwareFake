package co.edu.unbosque.marketplace.dto;

/**
 * Política de cancelación con sus reglas de devolución según los días de
 * anticipación con que se cancela.
 */
public class PoliticaCancelacionDTO {
	/** Identificador de la política. */
	private Long idPolitica;

	/** Nombre: FLEXIBLE, MODERADA o ESTRICTA. */
	private String nombre;

	/** Explicación para el huésped de cuánto se devuelve y cuándo. */
	private String condiciones;

	/**
	 * Días mínimos de anticipación para devolver el 100 %; {@code null} si no
	 * aplica.
	 */
	private Integer diasReembolsoTotal;

	/**
	 * Días mínimos de anticipación para la devolución parcial; {@code null} si
	 * no aplica.
	 */
	private Integer diasReembolsoParcial;

	/** Porcentaje que se devuelve en el tramo parcial. */
	private Integer porcentajeParcial;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public PoliticaCancelacionDTO() {
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
	 * Devuelve el nombre: FLEXIBLE, MODERADA o ESTRICTA.
	 *
	 * @return nombre: FLEXIBLE, MODERADA o ESTRICTA
	 */
	public String getNombre() {
		return nombre;
	}

	/**
	 * Asigna el nombre: FLEXIBLE, MODERADA o ESTRICTA.
	 *
	 * @param nombre nombre: FLEXIBLE, MODERADA o ESTRICTA
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
