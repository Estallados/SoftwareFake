import { OrdenAlojamientos } from './ordenAlojamientosDto';

/** Filtros opcionales y combinables de GET /alojamientos. */
export interface FiltrosBusqueda {
  ciudad?: string;
  /** 'AAAA-MM-DD' */
  llegada?: string;
  /** 'AAAA-MM-DD' */
  salida?: string;
  huespedes?: number;
  tipo?: string;
  precioMinimo?: number;
  precioMaximo?: number;
  amenidades?: string[];
  orden?: OrdenAlojamientos;
}
