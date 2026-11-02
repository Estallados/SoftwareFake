/** Estados de una reseña: OCULTA la retira de la vista pública, pero su autor la sigue viendo. */
export const ESTADOS_RESENA = ['PUBLICADA', 'OCULTA'] as const;
export type EstadoResena = (typeof ESTADOS_RESENA)[number];
