/** Motivo de una acción (rechazo, bloqueo, restricción, cancelación, moderación). */
export interface Motivo {
  /** Explicación de la acción; queda en la auditoría y se le notifica al afectado. */
  motivo?: string;
}
