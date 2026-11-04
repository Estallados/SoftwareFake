package co.edu.unbosque.marketplace.entity;

/**
 * Imagen de un alojamiento. No se guarda en la base de datos: el archivo y estos
 * datos viven en Cloudinary ({@code rutaArchivo} es su id allá) y la aplicación
 * los carga en memoria al arrancar. Una imagen por alojamiento es la principal
 * (portada).
 *
 * @see "RF-017"
 */
public class Imagen {
	private Long idImagen;
	private String url;
	private Long idAlojamiento;
	private Boolean principal;
	/** Id (public_id) de la imagen en Cloudinary. */
	private String rutaArchivo;
	/** Posición de la imagen dentro de su alojamiento, en orden de carga. */
	private long orden;

	public Imagen() {
	}

	/** Copia de otra imagen, para no compartir objetos con el almacén en memoria. */
	public Imagen(Imagen otra) {
		this.idImagen = otra.idImagen;
		this.url = otra.url;
		this.idAlojamiento = otra.idAlojamiento;
		this.principal = otra.principal;
		this.rutaArchivo = otra.rutaArchivo;
		this.orden = otra.orden;
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

	public long getOrden() {
		return orden;
	}

	public void setOrden(long orden) {
		this.orden = orden;
	}
}
