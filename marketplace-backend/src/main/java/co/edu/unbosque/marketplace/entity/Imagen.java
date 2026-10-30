package co.edu.unbosque.marketplace.entity;

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
 * Entidad que representa imagen en el marketplace. Puede ser un archivo subido
 * por el anfitrión (su contenido está en {@link ArchivoImagen} con la misma
 * {@code rutaArchivo}) o
 * una URL externa ({@code rutaArchivo} en null). Una imagen por alojamiento es
 * la principal (portada).
 *
 * @see "RF-017"
 */
@Entity
@Table(name = "imagen")
public class Imagen {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long idImagen;
	private String url;
	@Column(name = "id_alojamiento")
	private Long idAlojamiento;
	/**
	 * Clave foránea hacia {@link Alojamiento}; el valor se asigna con
	 * {@code idAlojamiento}.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "id_alojamiento", insertable = false, updatable = false)
	private Alojamiento alojamiento;
	/** Portada del alojamiento; null en imágenes anteriores a este campo. */
	private Boolean principal;
	/**
	 * Ruta relativa del archivo en el almacenamiento; null si es una URL externa.
	 */
	private String rutaArchivo;

	public Imagen() {
	}

	public Long getIdImagen() {
		return idImagen;
	}

	public void setIdImagen(Long idImagen) {
		this.idImagen = idImagen;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	public Boolean getPrincipal() {
		return principal;
	}

	public void setPrincipal(Boolean principal) {
		this.principal = principal;
	}

	public String getRutaArchivo() {
		return rutaArchivo;
	}

	public void setRutaArchivo(String rutaArchivo) {
		this.rutaArchivo = rutaArchivo;
	}
}
