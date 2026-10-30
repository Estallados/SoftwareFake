package co.edu.unbosque.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Contenido de una imagen subida. Se guarda en la base y no en el disco del
 * servidor para que todo el equipo, que comparte la base, vea las mismas
 * imágenes. Va en su propia tabla para que listar {@link Imagen} no cargue los
 * bytes.
 *
 * @see "RF-017"
 */
@Entity
@Table(name = "archivo_imagen")
public class ArchivoImagen {
	/** La misma ruta de {@link Imagen#getRutaArchivo()}. */
	@Id
	private String ruta;
	/** image/jpeg, image/png o image/webp. */
	private String tipoContenido;
	@Column(nullable = false)
	private byte[] datos;

	public ArchivoImagen() {
	}

	public ArchivoImagen(String ruta, String tipoContenido, byte[] datos) {
		this.ruta = ruta;
		this.tipoContenido = tipoContenido;
		this.datos = datos;
	}

	public String getRuta() {
		return ruta;
	}

	public void setRuta(String ruta) {
		this.ruta = ruta;
	}

	public String getTipoContenido() {
		return tipoContenido;
	}

	public void setTipoContenido(String tipoContenido) {
		this.tipoContenido = tipoContenido;
	}

	public byte[] getDatos() {
		return datos;
	}

	public void setDatos(byte[] datos) {
		this.datos = datos;
	}
}
