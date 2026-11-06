import { ServicioAdicional } from './servicioAdicionalDto';
/** Cuerpo de POST /cotizaciones (CotizacionDTO de entrada). Fechas ISO "YYYY-MM-DD". */
export interface CotizacionSolicitud {
  /** Alojamiento a cotizar. */
  idAlojamiento: number;
  /** Día de llegada. */
  fechaEntrada: string;
  /** Día de salida. */
  fechaSalida: string;
  /** Cantidad de huéspedes. */
  huespedes: number;
  /** Ids de los servicios adicionales elegidos. */
  serviciosSeleccionados: number[];
}
/** Respuesta de POST /cotizaciones: desglose calculado por el backend (vale 30 min). */
export interface Cotizacion {
  /** Identificador de la cotización. */
  idCotizacion: number;
  /** Alojamiento cotizado. */
  idAlojamiento: number;
  /** Día de llegada. */
  fechaEntrada: string;
  /** Día de salida. */
  fechaSalida: string;
  /** Cantidad de huéspedes. */
  huespedes: number;
  /** Cantidad de noches. */
  noches: number;
  /** Precio por noche. */
  precioNoche: number;
  /** Precio por noche multiplicado por las noches. */
  precioBase: number;
  /** Descuento aplicado. */
  descuento: number;
  /** Porcentaje de limpieza aplicado. */
  porcentajeLimpieza: number;
  /** Valor de la limpieza. */
  valorLimpieza: number;
  /** Porcentaje de servicio aplicado. */
  porcentajeServicio: number;
  /** Valor de la tarifa de servicio. */
  valorServicio: number;
  /** Suma de los servicios adicionales. */
  valorAdicionales: number;
  /** Valor total a pagar. */
  total: number;
  /** Ids de los servicios adicionales elegidos. */
  serviciosSeleccionados: number[] | null;
  /** Servicios adicionales incluidos, con su costo. */
  serviciosAdicionales: ServicioAdicional[] | null;
  /** Todos los servicios adicionales que ofrece el alojamiento. */
  serviciosDisponibles: ServicioAdicional[] | null;
  /** LocalDateTime sin zona horaria, ej. "2026-01-01T17:37:00". */
  fechaExpiracion: string;
  /** Reserva creada con esta cotización; null si aún no se usa. */
  idReserva: number | null;
}
