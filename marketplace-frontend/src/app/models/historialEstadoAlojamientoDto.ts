import { EstadoAlojamiento } from './estadoAlojamientoDto';

/** Un cambio de estado de un alojamiento (HistorialEstadoAlojamientoDTO). */
export interface HistorialEstadoAlojamiento {
  idHistorial: number;
  idAlojamiento: number;
  estadoAnterior: EstadoAlojamiento | null;
  estadoNuevo: EstadoAlojamiento;
  motivo?: string;
  idUsuario: number;
  /** 'AAAA-MM-DDTHH:mm:ss' */
  fecha: string;
}
