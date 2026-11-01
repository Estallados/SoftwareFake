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
  fechaEntrada: string;
  fechaSalida: string;
  noches: number;
  huespedes: number;
  /** Ej. "CONFIRMADA", "CANCELADA". */
  estado: string;
  total: number;
  /** JSON de la cotización serializado como texto. */
  desglose: string;
  idHuesped: number;
  idAlojamiento: number;
  tituloAlojamiento: string;
  idCotizacion: number;
  fechaCreacion: string;
  completada: boolean;
  pago: Pago | null;
  imagenAlojamiento: string | null;
  ciudad: string;
  /** Dirección exacta: solo se comparte con la reserva confirmada. */
  ubicacion: string;
  tipoAlojamiento: string;
  capacidadAlojamiento: number;
  nombrePolitica: string;
  nombreHuesped: string;
  idAnfitrion: number;
  nombreAnfitrion: string;
  tieneResena: boolean;
  cancelacion: unknown | null;
}
