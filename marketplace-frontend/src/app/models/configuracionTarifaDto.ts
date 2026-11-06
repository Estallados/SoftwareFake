/**
 * Porcentajes globales de limpieza y servicio. Si porDefecto es true, todavía no hay configuración
 * guardada y se usan los valores por defecto.
 */
export interface ConfiguracionTarifa {
  /** Porcentaje de limpieza sobre el precio base. */
  porcentajeLimpieza: number;
  /** Porcentaje de la tarifa de servicio sobre el precio base. */
  porcentajeServicio: number;
  /** Los asigna el backend; no hace falta enviarlos al actualizar. */
  idUsuario?: number;
  /** Momento del último cambio (ISO). */
  fecha?: string; // ISO date-time
  /** Indica si son los valores por defecto. */
  porDefecto?: boolean;
}
