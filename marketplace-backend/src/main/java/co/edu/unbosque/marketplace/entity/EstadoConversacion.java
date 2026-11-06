package co.edu.unbosque.marketplace.entity;

/**
 * Estados de una conversación entre un huésped y un anfitrión.
 */
public enum EstadoConversacion {

	/** Se pueden enviar mensajes. */
	ACTIVA,

	/** La conversación terminó; ya no admite mensajes nuevos. */
	CERRADA
}
