import { ServicioAdicional } from './servicioAdicionalDto';

export interface CotizacionSolicitud {
 
  idAlojamiento: number;

  fechaEntrada: string;

  fechaSalida: string;

  huespedes: number;
  
  serviciosSeleccionados: number[];
}

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

  fechaExpiracion: string;
 
  idReserva: number | null;
}
