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
 * Entidad que registra cada exportación de reporte: quién la pidió y cuándo.
 *
 * @see "RF-006, CA-029"
 */
@Entity
@Table(name = "registroExportacion")
public class RegistroExportacion {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idRegistro;
	private String tipoReporte;
	private String formato;
	private Integer cantidadRegistros;
	@Column(name = "id_usuario")
	private Long idUsuario;
	/**
	 * Clave foránea hacia {@link Usuario}; el valor se asigna con
	 * {@code idUsuario}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_usuario", insertable = false, updatable = false)
	private Usuario usuario;
	private LocalDateTime fecha;

	public RegistroExportacion() {
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

	public LocalDateTime getFecha() {
		return fecha;
	}

	public void setFecha(LocalDateTime fecha) {
		this.fecha = fecha;
	}
}
