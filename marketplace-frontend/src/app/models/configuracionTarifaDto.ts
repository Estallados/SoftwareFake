
export interface ConfiguracionTarifa {
  
  porcentajeLimpieza: number;
 
  porcentajeServicio: number;
  
  idUsuario?: number;
 
  fecha?: string; // ISO date-time
 
  porDefecto?: boolean;
}
