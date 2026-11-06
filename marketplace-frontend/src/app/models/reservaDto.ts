import { Cancelacion } from './cancelacionDto';
import { EstadoReserva } from './estadoReservaDto';
import { Pago } from './pagoDto';

/** Cuerpo de POST /reservas. */
export interface SolicitudReserva {
  /** Cotización vigente con la que se crea la reserva. */
  idCotizacion: number;
  /** PaymentMethod de Stripe (pm_...): de prueba o el creado con Stripe Elements. */
  metodoPago: string;
}

/** Respuesta de POST /reservas y GET /reservas/{id}. */
export interface Reserva {
  /** Código de la reserva. */
  idReserva: number;
  /** YYYY-MM-DD. */
  fechaEntrada: string;
  /** YYYY-MM-DD. */
  fechaSalida: string;
  /** Cantidad de noches. */
  noches: number;
  /** Cantidad de huéspedes. */
  huespedes: number;
  /** Estado de la reserva. */
  estado: EstadoReserva;
  /** Valor total. */
  total: number;
  /** JSON de la cotización serializado como texto. */
  desglose: string;
  /** Huésped que reservó. */
  idHuesped: number;
  /** Alojamiento reservado. */
  idAlojamiento: number;
  /** Título del alojamiento. */
  tituloAlojamiento: string;
  /** Cotización de la que salió el precio. */
  idCotizacion: number;
  /** ISO date-time. */
  fechaCreacion: string;
  /** true si está CONFIRMADA y la fecha de salida ya pasó. */
  completada: boolean;
  /** Último pago; null si aún no hay. */
  pago: Pago | null;
  /** URL de la imagen principal del alojamiento. */
  imagenAlojamiento: string | null;
  /** Ciudad del alojamiento. */
  ciudad: string;
  /** Dirección exacta: solo se comparte con la reserva confirmada. */
  ubicacion: string | null;
  /** Tipo de alojamiento. */
  tipoAlojamiento: string;
  /** Capacidad máxima del alojamiento. */
  capacidadAlojamiento: number;
  /** Política de cancelación del alojamiento. */
  nombrePolitica: string;
  /** Nombre del huésped. */
  nombreHuesped: string;
  /** Anfitrión del alojamiento. */
  idAnfitrion: number;
  /** Nombre del anfitrión. */
  nombreAnfitrion: string;
  /** Si el huésped ya reseñó esta reserva. */
  tieneResena: boolean;
  /** Datos de la cancelación; null si no fue cancelada por el huésped. */
  cancelacion: Cancelacion | null;
}
