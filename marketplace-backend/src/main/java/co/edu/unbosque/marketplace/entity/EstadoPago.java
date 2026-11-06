package co.edu.unbosque.marketplace.entity;

/**
 * Estados de un pago. Los pagos se hacen en modo de prueba (simulados o con
 * Stripe en modo test); no se mueve dinero real.
 */
public enum EstadoPago {

	/** El pago se creó y espera la respuesta de la pasarela. */
	PENDIENTE,

	/** La pasarela aprobó el pago. */
	APROBADO,

	/** La pasarela rechazó el pago (por ejemplo, tarjeta sin fondos). */
	RECHAZADO,

	/** No se pudo completar la comunicación con la pasarela. */
	ERROR,

	/** El pago se devolvió, total o parcialmente, al cancelar la reserva. */
	REEMBOLSADO
}
