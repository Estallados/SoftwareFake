package co.edu.unbosque.marketplace.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Porcentajes globales de las tarifas de limpieza y de servicio que se suman a
 * cada cotización. Cada cambio inserta una fila nueva, así queda el historial;
 * la configuración vigente es la más reciente.
 */
@Entity
@Table(name = "configuracionTarifa")
public class ConfiguracionTarifa {

	/** Identificador de la configuración. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idConfiguracion;

	/** Porcentaje sobre el precio base que se cobra por limpieza. */
	private BigDecimal porcentajeLimpieza;

	/** Porcentaje sobre el precio base que se cobra como tarifa de servicio. */
	private BigDecimal porcentajeServicio;

	/** Administrador que hizo el cambio. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Momento en que se guardó la configuración. */
	private LocalDateTime fecha;

	/** Constructor vacío, requerido por JPA. */
	public ConfiguracionTarifa() {
	}

	/**
	 * Devuelve el identificador de la configuración.
	 *
	 * @return identificador de la configuración
	 */
	public Long getIdConfiguracion() {
		return idConfiguracion;
	}

	/**
	 * Asigna el identificador de la configuración.
	 *
	 * @param idConfiguracion identificador de la configuración
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
	 * Devuelve el administrador que hizo el cambio.
	 *
	 * @return administrador que hizo el cambio
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el administrador que hizo el cambio.
	 *
	 * @param idUsuario administrador que hizo el cambio
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el momento en que se guardó la configuración.
	 *
	 * @return momento en que se guardó la configuración
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento en que se guardó la configuración.
	 *
	 * @param fecha momento en que se guardó la configuración
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
