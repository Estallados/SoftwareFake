/** Evento de auditoría para consulta del administrador. */
export interface EventoAuditoria {
  
  idEvento: number;

  accion: string;
  
  fecha: string; // ISO date-time
  
  detalle?: string;
  
  idUsuario?: number;
 
  entidad?: string;
  
  idEntidad?: number;
  
  resultado: string;
}
