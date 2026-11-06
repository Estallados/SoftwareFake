package co.edu.unbosque.marketplace.dto;

/**
 * Reseñas visibles de un alojamiento con su calificación promedio y la
 * cantidad total. También se usa para todas las reseñas de un anfitrión; en
 * ese caso idAlojamiento va en null.
 */
public class ResumenResenasDTO {
	/**
	 * Alojamiento al que pertenecen; {@code null} si son todas las de un
	 * anfitrión.
	 */
	private Long idAlojamiento;

	/** Calificación promedio de las reseñas visibles. */
	private Double promedio;

	/** Cantidad de reseñas visibles. */
	private Long cantidad;

	/** Página de reseñas, de la más reciente a la más antigua. */
	private PaginaDTO<ResenaDTO> resenas;

	/** Constructor vacío, necesario para convertir desde y hacia JSON. */
	public ResumenResenasDTO() {
	}

	/**
	 * Devuelve el alojamiento al que pertenecen; {@code null} si son todas las de un anfitrión.
	 *
	 * @return alojamiento al que pertenecen; {@code null} si son todas las de un anfitrión
	 */
	public Long getIdAlojamiento() {
		return idAlojamiento;
	}

	/**
	 * Asigna el alojamiento al que pertenecen; {@code null} si son todas las de un anfitrión.
	 *
	 * @param idAlojamiento alojamiento al que pertenecen; {@code null} si son todas las de un anfitrión
	 */
	public void setIdAlojamiento(Long idAlojamiento) {
		this.idAlojamiento = idAlojamiento;
	}

	/**
	 * Devuelve la calificación promedio de las reseñas visibles.
	 *
	 * @return calificación promedio de las reseñas visibles
	 */
	public Double getPromedio() {
		return promedio;
	}

	/**
	 * Asigna la calificación promedio de las reseñas visibles.
	 *
	 * @param promedio calificación promedio de las reseñas visibles
	 */
	public void setPromedio(Double promedio) {
		this.promedio = promedio;
	}

	/**
	 * Devuelve la cantidad de reseñas visibles.
	 *
	 * @return cantidad de reseñas visibles
	 */
	public Long getCantidad() {
		return cantidad;
	}

	/**
	 * Asigna la cantidad de reseñas visibles.
	 *
	 * @param cantidad cantidad de reseñas visibles
	 */
	public void setCantidad(Long cantidad) {
		this.cantidad = cantidad;
	}

	/**
	 * Devuelve la página de reseñas, de la más reciente a la más antigua.
	 *
	 * @return página de reseñas, de la más reciente a la más antigua
	 */
	public PaginaDTO<ResenaDTO> getResenas() {
		return resenas;
	}

	/**
	 * Asigna la página de reseñas, de la más reciente a la más antigua.
	 *
	 * @param resenas página de reseñas, de la más reciente a la más antigua
	 */
	public void setResenas(PaginaDTO<ResenaDTO> resenas) {
		this.resenas = resenas;
	}
}
