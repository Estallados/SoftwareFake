import { ServicioAdicional } from './servicioAdicionalDto';
/** Cuerpo de POST /cotizaciones (CotizacionDTO de entrada). Fechas ISO "YYYY-MM-DD". */
export interface CotizacionSolicitud {
  idAlojamiento: number;
  fechaEntrada: string;
  fechaSalida: string;
  huespedes: number;
  /** Ids de los servicios adicionales elegidos. */
  serviciosSeleccionados: number[];
}
/** Respuesta de POST /cotizaciones: desglose calculado por el backend (vale 30 min). */
export interface Cotizacion {
  idCotizacion: number;
  idAlojamiento: number;
  fechaEntrada: string;
  fechaSalida: string;
  huespedes: number;
  noches: number;
  precioNoche: number;
  precioBase: number;
  descuento: number;
  porcentajeLimpieza: number;
  valorLimpieza: number;
  porcentajeServicio: number;
  valorServicio: number;
  valorAdicionales: number;
  total: number;
  serviciosSeleccionados: number[] | null;
  serviciosAdicionales: ServicioAdicional[] | null;
  serviciosDisponibles: ServicioAdicional[] | null;
  /** LocalDateTime sin zona horaria, ej. "2026-01-01T17:37:00". */
  fechaExpiracion: string;
  idReserva: number | null;
}
