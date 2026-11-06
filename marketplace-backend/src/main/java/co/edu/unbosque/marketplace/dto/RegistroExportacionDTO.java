package co.edu.unbosque.marketplace.dto;

import java.time.LocalDateTime;

/**
 * Una exportación de reporte del historial: qué se exportó, en qué formato,
 * cuántos registros tenía, quién la pidió y cuándo.
 */
public class RegistroExportacionDTO {
	/** Identificador del registro. */
	private Long idRegistro;

	/** Reporte exportado. */
	private String tipoReporte;

	/** Formato del archivo: PDF o EXCEL. */
	private String formato;

	/** Cantidad de filas del reporte. */
	private Integer cantidadRegistros;

	/** Administrador que lo pidió. */
	private Long idUsuario;

	/** Nombre del administrador que lo pidió. */
	private String nombreUsuario;

	/** Momento de la exportación. */
	private LocalDateTime fecha;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public RegistroExportacionDTO() {
	}

	/**
	 * Devuelve el identificador del registro.
	 *
	 * @return identificador del registro
	 */
	public Long getIdRegistro() {
		return idRegistro;
	}

	/**
	 * Asigna el identificador del registro.
	 *
	 * @param idRegistro identificador del registro
	 */
	public void setIdRegistro(Long idRegistro) {
		this.idRegistro = idRegistro;
	}

	/**
	 * Devuelve el reporte exportado.
	 *
	 * @return reporte exportado
	 */
	public String getTipoReporte() {
		return tipoReporte;
	}

	/**
	 * Asigna el reporte exportado.
	 *
	 * @param tipoReporte reporte exportado
	 */
	public void setTipoReporte(String tipoReporte) {
		this.tipoReporte = tipoReporte;
	}

	/**
	 * Devuelve el formato del archivo: PDF o EXCEL.
	 *
	 * @return formato del archivo: PDF o EXCEL
	 */
	public String getFormato() {
		return formato;
	}

	/**
	 * Asigna el formato del archivo: PDF o EXCEL.
	 *
	 * @param formato formato del archivo: PDF o EXCEL
	 */
	public void setFormato(String formato) {
		this.formato = formato;
	}

	/**
	 * Devuelve la cantidad de filas del reporte.
	 *
	 * @return cantidad de filas del reporte
	 */
	public Integer getCantidadRegistros() {
		return cantidadRegistros;
	}

	/**
	 * Asigna la cantidad de filas del reporte.
	 *
	 * @param cantidadRegistros cantidad de filas del reporte
	 */
	public void setCantidadRegistros(Integer cantidadRegistros) {
		this.cantidadRegistros = cantidadRegistros;
	}

	/**
	 * Devuelve el administrador que lo pidió.
	 *
	 * @return administrador que lo pidió
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el administrador que lo pidió.
	 *
	 * @param idUsuario administrador que lo pidió
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
	}

	/**
	 * Devuelve el nombre del administrador que lo pidió.
	 *
	 * @return nombre del administrador que lo pidió
	 */
	public String getNombreUsuario() {
		return nombreUsuario;
	}

	/**
	 * Asigna el nombre del administrador que lo pidió.
	 *
	 * @param nombreUsuario nombre del administrador que lo pidió
	 */
	public void setNombreUsuario(String nombreUsuario) {
		this.nombreUsuario = nombreUsuario;
	}

	/**
	 * Devuelve el momento de la exportación.
	 *
	 * @return momento de la exportación
	 */
	public LocalDateTime getFecha() {
		return fecha;
	}

	/**
	 * Asigna el momento de la exportación.
	 *
	 * @param fecha momento de la exportación
	 */
	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
