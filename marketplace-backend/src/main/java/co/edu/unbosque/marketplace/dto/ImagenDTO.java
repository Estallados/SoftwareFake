package co.edu.unbosque.marketplace.dto;

/**
 * Imagen de un alojamiento: su URL pública y si es la principal (portada).
 *
 * @see "RF-017"
 */
public class ImagenDTO {
	private Long idImagen;
	private String url;
	private Long idAlojamiento;
	private Boolean principal;
	/** Id del archivo en Cloudinary. */
	private String rutaArchivo;

	public ImagenDTO() {
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
