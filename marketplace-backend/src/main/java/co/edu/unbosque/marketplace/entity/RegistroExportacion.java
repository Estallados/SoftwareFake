package co.edu.unbosque.marketplace.entity;

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
 * Registro de cada exportación de reporte que hace un administrador: qué
 * reporte, en qué formato, cuántos registros tenía, quién lo pidió y cuándo.
 */
@Entity
@Table(name = "registroExportacion")
public class RegistroExportacion {

	/** Identificador del registro. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idRegistro;

	/** Reporte exportado: usuarios, alojamientos, reservas, ingresos o demanda. */
	private String tipoReporte;

	/** Formato del archivo: PDF o EXCEL. */
	private String formato;

	/** Cantidad de filas que tenía el reporte. */
	private Integer cantidadRegistros;

	/** Administrador que pidió la exportación. */
	@Column(name = "id_usuario")
	private Long idUsuario;

	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;

	/** Momento de la exportación. */
	private LocalDateTime fecha;

	/** Constructor vacío, requerido por JPA. */
	public RegistroExportacion() {
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
	 * Devuelve el reporte exportado: usuarios, alojamientos, reservas, ingresos o demanda.
	 *
	 * @return reporte exportado: usuarios, alojamientos, reservas, ingresos o demanda
	 */
	public String getTipoReporte() {
		return tipoReporte;
	}

	/**
	 * Asigna el reporte exportado: usuarios, alojamientos, reservas, ingresos o demanda.
	 *
	 * @param tipoReporte reporte exportado: usuarios, alojamientos, reservas, ingresos o demanda
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
	 * Devuelve la cantidad de filas que tenía el reporte.
	 *
	 * @return cantidad de filas que tenía el reporte
	 */
	public Integer getCantidadRegistros() {
		return cantidadRegistros;
	}

	/**
	 * Asigna la cantidad de filas que tenía el reporte.
	 *
	 * @param cantidadRegistros cantidad de filas que tenía el reporte
	 */
	public void setCantidadRegistros(Integer cantidadRegistros) {
		this.cantidadRegistros = cantidadRegistros;
	}

	/**
	 * Devuelve el administrador que pidió la exportación.
	 *
	 * @return administrador que pidió la exportación
	 */
	public Long getIdUsuario() {
		return idUsuario;
	}

	/**
	 * Asigna el administrador que pidió la exportación.
	 *
	 * @param idUsuario administrador que pidió la exportación
	 */
	public void setIdUsuario(Long idUsuario) {
		this.idUsuario = idUsuario;
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
