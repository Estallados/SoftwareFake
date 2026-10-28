package co.edu.unbosque.marketplace.entity;

/**
 * Estados de una publicación de alojamiento. Solo se permiten estos cinco.
 *
 * @see "RF-018, RF-027, CA-104"
 */
public enum EstadoAlojamiento {
	BORRADOR, PENDIENTE_REVISION, ACTIVA, PAUSADA, BLOQUEADA
}
