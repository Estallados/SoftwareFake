/** Evento de auditoría para consulta del administrador. */
export interface EventoAuditoria {
  /** Identificador del evento. */
  idEvento: number;
  /** Acción realizada, por ejemplo LOGIN. */
  accion: string;
  /** Momento del evento (ISO). */
  fecha: string; // ISO date-time
  /** Información adicional del evento. */
  detalle?: string;
  /** Usuario que hizo la acción; vacío si no se conoce. */
  idUsuario?: number;
  /** Tipo de recurso afectado, por ejemplo USUARIO. */
  entidad?: string;
  /** Id del recurso afectado. */
  idEntidad?: number;
  /** Valores de ResultadoAuditoria (enum del backend). */
  resultado: string;
}
