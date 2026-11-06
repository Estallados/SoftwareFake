import { EstadoAlojamiento } from './estadoAlojamientoDto';

/** Un cambio de estado de un alojamiento (HistorialEstadoAlojamientoDTO). */
export interface HistorialEstadoAlojamiento {
  /** Identificador del registro. */
  idHistorial: number;
  /** Alojamiento que cambió de estado. */
  idAlojamiento: number;
  /** Estado antes del cambio; null al crearlo. */
  estadoAnterior: EstadoAlojamiento | null;
  /** Estado después del cambio. */
  estadoNuevo: EstadoAlojamiento;
  /** Motivo del cambio. */
  motivo?: string;
  /** Usuario que hizo el cambio. */
  idUsuario: number;
  /** 'AAAA-MM-DDTHH:mm:ss' */
  fecha: string;
}
