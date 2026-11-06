package co.edu.unbosque.marketplace.entity;

/**
 * Estados de una reserva. No hay un estado "finalizada": una reserva se
 * considera completada cuando está CONFIRMADA y su fecha de salida ya pasó.
 */
public enum EstadoReserva {

	/** La reserva se creó y espera el pago; si no se paga a tiempo se cancela sola. */
	PENDIENTE_PAGO,

	/** El pago fue aprobado y las fechas quedan ocupadas. */
	CONFIRMADA,

	/** El administrador bloqueó el alojamiento y la reserva quedó en revisión. */
	EN_REVISION,

	/** La reserva se canceló (por el huésped, por falta de pago o por el sistema). */
	CANCELADA
}
