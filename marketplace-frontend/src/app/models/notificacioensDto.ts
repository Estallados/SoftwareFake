/** Notificación dentro de la plataforma, la que se ve en la campana. */
export interface Notificacion {
  /** Identificador de la notificación. */
  idNotificacion: number;
  /** Título de la notificación. */
  titulo?: string;
  /** Texto que ve el usuario. */
  mensaje?: string;
  /** Evento que la originó, por ejemplo RESERVA_CONFIRMADA. */
  tipo?: string;
  /** Momento en que se generó (ISO). */
  fecha?: string; // ISO date-time
  /** Indica si el usuario ya la leyó. */
  leida: boolean;
}
