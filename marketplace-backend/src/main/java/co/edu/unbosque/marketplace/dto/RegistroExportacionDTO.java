package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Una exportación de reporte del historial: qué se exportó, en qué formato,
 * cuántos registros, quién y cuándo.
 *
 * @see "RF-006, CA-029"
 */
public class RegistroExportacionDTO {
	private Long idRegistro;
	private String tipoReporte;
	private String formato;
	private Integer cantidadRegistros;
	private Long idUsuario;
	private String nombreUsuario;
	private LocalDateTime fecha;

	public RegistroExportacionDTO() {
	}

	public Long getIdRegistro() {
		return idRegistro;
	}

	public void setIdRegistro(Long idRegistro) {
		this.idRegistro = idRegistro;
	}

	public String getTipoReporte() {
		return tipoReporte;
	}

	public void setTipoReporte(String tipoReporte) {
		this.tipoReporte = tipoReporte;
	}

	public String getFormato() {
		return formato;
	}

	public void setFormato(String formato) {
		this.formato = formato;
	}

	public Integer getCantidadRegistros() {
		return cantidadRegistros;
	}

	public void setCantidadRegistros(Integer cantidadRegistros) {
		this.cantidadRegistros = cantidadRegistros;
	}

	public Long getIdUsuario() {
		return idUsuario;
	}

	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}

	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
