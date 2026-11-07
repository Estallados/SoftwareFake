
export const ESTADOS_RESERVA = ['PENDIENTE_PAGO', 'CONFIRMADA', 'EN_REVISION', 'CANCELADA'] as const;

export type EstadoReserva = (typeof ESTADOS_RESERVA)[number];
