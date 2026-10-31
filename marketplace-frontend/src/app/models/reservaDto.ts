import { Cancelacion } from './cancelacionDto';
import { EstadoReserva } from './estadoReservaDto';
import { Pago } from './pagoDto';

/**
 * Reserva con su desglose congelado, el último pago y los datos del alojamiento,
 * del huésped y del anfitrión que muestran las pantallas de reservas.
 */
export interface Reserva {
  idReserva: number;
  fechaEntrada: string; // YYYY-MM-DD
  fechaSalida: string; // YYYY-MM-DD
  noches: number;
  huespedes: number;
  estado: EstadoReserva;
  total: number;
  desglose?: string;
  idHuesped: number;
  idAlojamiento: number;
  tituloAlojamiento?: string;
  idCotizacion?: number;
  fechaCreacion?: string; // ISO date-time
  /** CONFIRMADA y con fecha de salida ya pasada. */
  completada?: boolean;
  pago?: Pago;
  /** URL de la imagen principal del alojamiento. */
  imagenAlojamiento?: string;
  ciudad?: string;
  ubicacion?: string;
  tipoAlojamiento?: string;
  capacidadAlojamiento?: number;
  /** Política de cancelación del alojamiento. */
  nombrePolitica?: string;
  nombreHuesped?: string;
  idAnfitrion?: number;
  nombreAnfitrion?: string;
  /** Si el huésped ya reseñó esta reserva. */
  tieneResena?: boolean;
  /** Datos de la cancelación; ausente si no fue cancelada por el huésped. */
  cancelacion?: Cancelacion;
}
