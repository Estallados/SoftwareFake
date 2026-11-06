import { OrdenAlojamientos } from './ordenAlojamientosDto';

/** Filtros opcionales y combinables de GET /alojamientos. */
export interface FiltrosBusqueda {
  /** Ciudad a buscar. */
  ciudad?: string;
  /** 'AAAA-MM-DD' */
  llegada?: string;
  /** 'AAAA-MM-DD' */
  salida?: string;
  /** Cantidad de huéspedes. */
  huespedes?: number;
  /** Tipo de alojamiento. */
  tipo?: string;
  /** Precio mínimo por noche. */
  precioMinimo?: number;
  /** Precio máximo por noche. */
  precioMaximo?: number;
  /** Amenidades que debe tener. */
  amenidades?: string[];
  /** Orden de los resultados. */
  orden?: OrdenAlojamientos;
}
