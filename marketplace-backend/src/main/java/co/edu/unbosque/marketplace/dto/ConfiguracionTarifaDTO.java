package co.edu.unbosque.marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Porcentajes globales de limpieza y servicio. porDefecto indica que no hay
 * configuración guardada y se usan los valores documentados.
 *
 * @see "RF-023, CA-085"
 */
public class ConfiguracionTarifaDTO {
	private Long idConfiguracion;
	private BigDecimal porcentajeLimpieza;
	private BigDecimal porcentajeServicio;
	private Long idUsuario;
	private LocalDateTime fecha;
	private Boolean porDefecto;

	public ConfiguracionTarifaDTO() {
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

	public Boolean getPorDefecto() {
		return porDefecto;
	}

	public void setPorDefecto(Boolean porDefecto) {
		this.porDefecto = porDefecto;
	}
}
