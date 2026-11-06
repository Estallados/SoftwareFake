package co.edu.unbosque.marketplace.entity;

/**
 * Estado de una petición registrada con la cabecera {@code Idempotency-Key}.
 * Sirve para que reenviar la misma petición (por un doble clic o un reintento)
 * no cree dos reservas ni cobre dos veces.
 */
public enum EstadoIdempotencia {

	/** La petición se está atendiendo; un reintento debe esperar. */
	EN_PROCESO,

	/** La petición ya terminó; un reintento recibe la misma respuesta guardada. */
	COMPLETADA
}
