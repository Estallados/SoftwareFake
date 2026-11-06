package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Porcentajes globales de limpieza y de servicio que se suman a cada
 * cotización. Si porDefecto es true, todavía no hay ninguna configuración
 * guardada y se están usando los valores por defecto.
 */
public class ConfiguracionTarifaDTO {
	/** Identificador de la configuración guardada. */
	private Long idConfiguracion;

	/** Porcentaje sobre el precio base que se cobra por limpieza. */
	private BigDecimal porcentajeLimpieza;

	/** Porcentaje sobre el precio base que se cobra como tarifa de servicio. */
	private BigDecimal porcentajeServicio;

	/** Administrador que hizo el último cambio. */
	private Long idUsuario;

	/** Momento del último cambio. */
	private LocalDateTime fecha;

	/**
	 * Indica si son los valores por defecto (no hay configuración guardada).
	 */
	private Boolean porDefecto;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ConfiguracionTarifaDTO() {
	}

	/**
	 * Devuelve el identificador de la configuración guardada.
	 *
	 * @return identificador de la configuración guardada
	 */
	public Long getIdConfiguracion() {
		return idConfiguracion;
	}

	/**
	 * Asigna el identificador de la configuración guardada.
	 *
	 * @param idConfiguracion identificador de la configuración guardada
	 */
	public void setIdConfiguracion(Long idConfiguracion) {
		this.idConfiguracion = idConfiguracion;
	}

	/**
	 * Devuelve el porcentaje sobre el precio base que se cobra por limpieza.
	 *
	 * @return porcentaje sobre el precio base que se cobra por limpieza
	 */
	public BigDecimal getPorcentajeLimpieza() {
		return porcentajeLimpieza;
	}

	/**
	 * Asigna el porcentaje sobre el precio base que se cobra por limpieza.
	 *
	 * @param porcentajeLimpieza porcentaje sobre el precio base que se cobra por limpieza
	 */
	public void setPorcentajeLimpieza(BigDecimal porcentajeLimpieza) {
		this.porcentajeLimpieza = porcentajeLimpieza;
	}

	/**
	 * Devuelve el porcentaje sobre el precio base que se cobra como tarifa de servicio.
	 *
	 * @return porcentaje sobre el precio base que se cobra como tarifa de servicio
	 */
	public BigDecimal getPorcentajeServicio() {
		return porcentajeServicio;
	}

	/**
	 * Asigna el porcentaje sobre el precio base que se cobra como tarifa de servicio.
	 *
	 * @param porcentajeServicio porcentaje sobre el precio base que se cobra como tarifa de servicio
	 */
	public void setPorcentajeServicio(BigDecimal porcentajeServicio) {
		this.porcentajeServicio = porcentajeServicio;
	}

	/**
	 * Devuelve el administrador que hizo el último cambio.
	 *
	 * @return administrador que hizo el último cambio
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el administrador que hizo el último cambio.
	 *
	 * @param idUsuario administrador que hizo el último cambio
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el momento del último cambio.
	 *
	 * @return momento del último cambio
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento del último cambio.
	 *
	 * @param fecha momento del último cambio
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}

	/**
	 * Devuelve {@code true} si son los valores por defecto (no hay configuración guardada).
	 *
	 * @return {@code true} si son los valores por defecto (no hay configuración guardada)
	 */
	public Boolean getPorDefecto() {
		return porDefecto;
	}

	/**
	 * Asigna {@code true} si son los valores por defecto (no hay configuración guardada).
	 *
	 * @param porDefecto {@code true} si son los valores por defecto (no hay configuración guardada)
	 */
	public void setPorDefecto(Boolean porDefecto) {
		this.porDefecto = porDefecto;
	}
}
