package co.edu.unbosque.marketplace.dto;

/**
 * Imagen de un alojamiento: su URL pública y si es la portada.
 */
public class ImagenDTO {
	/** Identificador de la imagen. */
	private Long idImagen;

	/** Dirección pública (HTTPS) de la imagen. */
	private String url;

	/** Alojamiento al que pertenece. */
	private Long idAlojamiento;

	/** Indica si es la portada del alojamiento. */
	private Boolean principal;
	/** Id del archivo en Cloudinary. */
	private String rutaArchivo;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ImagenDTO() {
	}

	/**
	 * Devuelve el identificador de la imagen.
	 *
	 * @return identificador de la imagen
	 */
	public Long getIdImagen() {
		return idImagen;
	}

	/**
	 * Asigna el identificador de la imagen.
	 *
	 * @param idImagen identificador de la imagen
	 */
	public void setIdImagen(Long idImagen) {
		this.idImagen = idImagen;
	}

	/**
	 * Devuelve la dirección pública (HTTPS) de la imagen.
	 *
	 * @return dirección pública (HTTPS) de la imagen
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Asigna la dirección pública (HTTPS) de la imagen.
	 *
	 * @param url dirección pública (HTTPS) de la imagen
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Devuelve el alojamiento al que pertenece.
	 *
	 * @return alojamiento al que pertenece
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento al que pertenece.
	 *
	 * @param idAlojamiento alojamiento al que pertenece
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
	 * Devuelve el id del archivo en Cloudinary.
	 *
	 * @return id del archivo en Cloudinary
	 */
	public String getRutaArchivo() {
		return rutaArchivo;
	}

	/**
	 * Asigna el id del archivo en Cloudinary.
	 *
	 * @param rutaArchivo id del archivo en Cloudinary
	 */
	public void setRutaArchivo(String rutaArchivo) {
		this.rutaArchivo = rutaArchivo;
	}
}
