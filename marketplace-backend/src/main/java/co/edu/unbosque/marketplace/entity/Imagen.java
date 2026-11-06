package co.edu.unbosque.marketplace.entity;

/**
 * Imagen de un alojamiento. No se guarda en la base de datos: el archivo y estos
 * datos viven en Cloudinary ({@code rutaArchivo} es su id allá) y la aplicación
 * los carga en memoria al arrancar. Una imagen por alojamiento es la principal
 * (portada).
 */
public class Imagen {

	/** Identificador de la imagen dentro de la aplicación. */
	private Long idImagen;

	/** Dirección pública (HTTPS) de la imagen en Cloudinary. */
	private String url;

	/** Alojamiento al que pertenece la imagen. */
	private Long idAlojamiento;

	/** Indica si es la portada del alojamiento. */
	private Boolean principal;

	/** Id (public_id) de la imagen en Cloudinary. */
	private String rutaArchivo;

	/** Posición de la imagen dentro de su alojamiento, en orden de carga. */
	private long orden;

	/** Constructor vacío. */
	public Imagen() {
	}

	/**
	 * Copia de otra imagen, para no compartir objetos con el almacén en memoria.
	 *
	 * @param otra imagen que se copia
	 */
	public Imagen(Imagen otra) {
		this.idImagen = otra.idImagen;
		this.url = otra.url;
		this.idAlojamiento = otra.idAlojamiento;
		this.principal = otra.principal;
		this.rutaArchivo = otra.rutaArchivo;
		this.orden = otra.orden;
	}

	/**
	 * Devuelve el identificador de la imagen dentro de la aplicación.
	 *
	 * @return identificador de la imagen dentro de la aplicación
	 */
	public Long getIdImagen() {
		return idImagen;
	}

	/**
	 * Asigna el identificador de la imagen dentro de la aplicación.
	 *
	 * @param idImagen identificador de la imagen dentro de la aplicación
	 */
	public void setIdImagen(Long idImagen) {
		this.idImagen = idImagen;
	}

	/**
	 * Devuelve la dirección pública (HTTPS) de la imagen en Cloudinary.
	 *
	 * @return dirección pública (HTTPS) de la imagen en Cloudinary
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Asigna la dirección pública (HTTPS) de la imagen en Cloudinary.
	 *
	 * @param url dirección pública (HTTPS) de la imagen en Cloudinary
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Devuelve el alojamiento al que pertenece la imagen.
	 *
	 * @return alojamiento al que pertenece la imagen
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento al que pertenece la imagen.
	 *
	 * @param idAlojamiento alojamiento al que pertenece la imagen
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve {@code true} si es la portada del alojamiento.
	 *
	 * @return {@code true} si es la portada del alojamiento
	 */
	public Boolean getPrincipal() {
		return principal;
	}

	/**
	 * Asigna {@code true} si es la portada del alojamiento.
	 *
	 * @param principal {@code true} si es la portada del alojamiento
	 */
	public void setPrincipal(Boolean principal) {
		this.principal = principal;
	}

	/**
	 * Devuelve el id (public_id) de la imagen en Cloudinary.
	 *
	 * @return id (public_id) de la imagen en Cloudinary
	 */
	public String getRutaArchivo() {
		return rutaArchivo;
	}

	/**
	 * Asigna el id (public_id) de la imagen en Cloudinary.
	 *
	 * @param rutaArchivo id (public_id) de la imagen en Cloudinary
	 */
	public void setRutaArchivo(String rutaArchivo) {
		this.rutaArchivo = rutaArchivo;
	}

	/**
	 * Devuelve la posición de la imagen dentro de su alojamiento, en orden de carga.
	 *
	 * @return posición de la imagen dentro de su alojamiento, en orden de carga
	 */
	public long getOrden() {
		return orden;
	}

	/**
	 * Asigna la posición de la imagen dentro de su alojamiento, en orden de carga.
	 *
	 * @param orden posición de la imagen dentro de su alojamiento, en orden de carga
	 */
	public void setOrden(long orden) {
		this.orden = orden;
	}
}
