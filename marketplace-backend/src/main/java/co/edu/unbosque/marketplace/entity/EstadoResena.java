package co.edu.unbosque.marketplace.entity;

/**
 * Visibilidad de una reseña. El administrador puede ocultarla sin modificar su
 * contenido.
 */
public enum EstadoResena {

	/** La reseña se muestra en el alojamiento y cuenta en su calificación. */
	PUBLICADA,

	/** Un administrador la ocultó: no se muestra ni cuenta en la calificación. */
	OCULTA
}
