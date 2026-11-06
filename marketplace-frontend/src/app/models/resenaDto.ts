import { EstadoResena } from './estadoResenaDto';

/** Reseña de una estancia. */
export interface Resena {
  /** Identificador de la reseña. */
  idResena: number;
  /** 1 a 5. */
  calificacion: number;
  /** Comentario del huésped. */
  comentario: string | null;
  /** Si está publicada u oculta. */
  estado: EstadoResena;
  /** ISO date-time. */
  fecha: string;
  /** Huésped que la escribió. */
  idUsuario: number;
  /** Reserva reseñada. */
  idReserva: number;
  /** Alojamiento reseñado. */
  idAlojamiento: number;
  /** Nombre del huésped que escribió la reseña. */
  nombreUsuario: string;
  /** Título del alojamiento reseñado. */
  tituloAlojamiento: string;
}

/** Cuerpo de POST /resenas (solo estos tres campos se usan de entrada). */
export interface SolicitudResena {
  /** Reserva que se reseña. */
  idReserva: number;
  /** 1 a 5. */
  calificacion: number;
  /** Hasta 2000 caracteres. */
  comentario?: string;
}

/**
 * Respuesta de GET /alojamientos/{id}/resenas: reseñas visibles con promedio y cantidad globales.
 * `resenas` es la página del backend; las reseñas están en `contenido`.
 */
export interface ResumenResenas {
  /** Página de reseñas. */
  resenas?: { contenido: Resena[] } | null;
  /** Calificación promedio. */
  promedio?: number | null;
  /** Cantidad de reseñas visibles. */
  cantidad?: number | null;
}
