/**
 * Porcentajes globales de limpieza y servicio.
 * porDefecto = true indica que no hay configuración guardada y se usan los valores documentados.
 */
export interface ConfiguracionTarifa {
  porcentajeLimpieza: number;
  porcentajeServicio: number;
  /** Los asigna el backend; no hace falta enviarlos al actualizar. */
  idUsuario?: number;
  fecha?: string; // ISO date-time
  porDefecto?: boolean;
}
