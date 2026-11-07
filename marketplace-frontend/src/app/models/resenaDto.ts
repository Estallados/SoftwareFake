import { EstadoResena } from './estadoResenaDto';

/** Reseña de una estancia. */
export interface Resena {
 
  idResena: number;
  /** 1 a 5. */
  calificacion: number;
 
  comentario: string | null;
 
  estado: EstadoResena;
  /** ISO date-time. */
  fecha: string;
 
  idUsuario: number;
 
  idReserva: number;
 
  idAlojamiento: number;
  /** Nombre del huésped que escribió la reseña. */
  nombreUsuario: string;
 
  tituloAlojamiento: string;
}

/** Cuerpo de POST /resenas (solo estos tres campos se usan de entrada). */
export interface SolicitudResena {
 
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
  
  resenas?: { contenido: Resena[] } | null;
  /
  promedio?: number | null;
 
  cantidad?: number | null;
}
