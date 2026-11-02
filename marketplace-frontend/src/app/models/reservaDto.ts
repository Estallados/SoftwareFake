import { Cancelacion } from './cancelacionDto';
import { EstadoReserva } from './estadoReservaDto';
import { Pago } from './pagoDto';

/** Cuerpo de POST /reservas. */
export interface SolicitudReserva {
  idCotizacion: number;
  /** PaymentMethod de Stripe (pm_...): de prueba o el creado con Stripe Elements. */
  metodoPago: string;
}

/** Respuesta de POST /reservas y GET /reservas/{id}. */
export interface Reserva {
  idReserva: number;
  /** YYYY-MM-DD. */
  fechaEntrada: string;
  /** YYYY-MM-DD. */
  fechaSalida: string;
  noches: number;
  huespedes: number;
  estado: EstadoReserva;
  total: number;
  /** JSON de la cotización serializado como texto. */
  desglose: string;
  idHuesped: number;
  idAlojamiento: number;
  tituloAlojamiento: string;
  idCotizacion: number;
  /** ISO date-time. */
  fechaCreacion: string;
  /** true si está CONFIRMADA y la fecha de salida ya pasó. */
  completada: boolean;
  /** Último pago; null si aún no hay. */
  pago: Pago | null;
  /** URL de la imagen principal del alojamiento. */
  imagenAlojamiento: string | null;
  ciudad: string;
  /** Dirección exacta: solo se comparte con la reserva confirmada. */
  ubicacion: string | null;
  tipoAlojamiento: string;
  capacidadAlojamiento: number;
  /** Política de cancelación del alojamiento. */
  nombrePolitica: string;
  nombreHuesped: string;
  idAnfitrion: number;
  nombreAnfitrion: string;
  /** Si el huésped ya reseñó esta reserva. */
  tieneResena: boolean;
  /** Datos de la cancelación; null si no fue cancelada por el huésped. */
  cancelacion: Cancelacion | null;
}
