package co.edu.unbosque.marketplace.entity;

/**
 * Estados de una reserva. Una reserva está "completada" cuando está CONFIRMADA
 * y su fecha de salida ya pasó.
 *
 * @see "RF-021, RF-022"
 */
public enum EstadoReserva {
    PENDIENTE_PAGO,
    CONFIRMADA,
    EN_REVISION,
    CANCELADA
}
