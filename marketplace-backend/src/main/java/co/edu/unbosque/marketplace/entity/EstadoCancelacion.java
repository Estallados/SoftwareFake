package co.edu.unbosque.marketplace.entity;

/**
 * Estados de una solicitud de cancelación de reserva.
 */
public enum EstadoCancelacion {

	/** El huésped pidió la cancelación. */
	SOLICITADA,

	/** La cancelación fue aceptada. */
	APROBADA,

	/** La cancelación fue rechazada. */
	RECHAZADA,

	/** La cancelación terminó y se calculó la devolución. */
	COMPLETADA
}
