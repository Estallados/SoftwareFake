/**
 * Estados de una reserva. "Completada" quiere decir CONFIRMADA con la fecha de salida ya pasada
 * (ver Reserva.completada).
 */
export const ESTADOS_RESERVA = ['PENDIENTE_PAGO', 'CONFIRMADA', 'EN_REVISION', 'CANCELADA'] as const;
/** Estado de una reserva (uno de {@link ESTADOS_RESERVA}). */
export type EstadoReserva = (typeof ESTADOS_RESERVA)[number];
