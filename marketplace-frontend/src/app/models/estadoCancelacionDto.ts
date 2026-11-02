/** Estados del registro de una cancelación. */
export const ESTADOS_CANCELACION = ['SOLICITADA', 'APROBADA', 'RECHAZADA', 'COMPLETADA'] as const;
export type EstadoCancelacion = (typeof ESTADOS_CANCELACION)[number];
